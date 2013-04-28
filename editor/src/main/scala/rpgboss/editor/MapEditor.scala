package rpgboss.editor

import rpgboss.lib._
import rpgboss.editor.misc.SwingUtils._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.editor.resourceselector._
import rpgboss.editor.uibase._
import rpgboss.editor.misc._
import rpgboss.editor.misc.GraphicsUtils._
import com.typesafe.scalalogging.slf4j.Logging
import scala.math._
import scala.swing._
import scala.swing.event._
import javax.imageio._
import java.awt.{ BasicStroke, AlphaComposite, Color }
import java.awt.geom.Line2D
import java.awt.event.MouseEvent
import rpgboss.model.event.RpgEvent
import rpgboss.editor.dialog.EventDialog
import java.awt.image.BufferedImage
import scala.collection.mutable.Buffer
import javax.swing.event._
import javax.swing.KeyStroke
import java.awt.event.KeyEvent
import java.awt.event.InputEvent
import rpgboss.editor.imageset.selector.TabbedTileSelector

class MapEditor(
  projectPanel: ProjectPanel,
  sm: StateMaster,
  tileSelector: TabbedTileSelector)
  extends MapView(projectPanel.mainP.topWin, sm, MapScales.scale1) {
  private var selectedLayer = MapLayers.default
  private var selectedTool = MapViewToolsEnum.default
  private var selectedEvtIdx: Option[Int] = None
  private var popupMenuOpen = false

  def selectLayer(layer: MapLayers.Value) = {
    selectedLayer = layer

    // Change display settings to make sense 
    botAlpha = 0.5f
    midAlpha = 0.5f
    topAlpha = 0.5f
    evtAlpha = 0.5f
    drawGrid = false

    import MapLayers._
    layer match {
      case Bot => botAlpha = 1.0f
      case Mid => midAlpha = 1.0f
      case Top => topAlpha = 1.0f
      case Evt =>
        botAlpha = 1.0f
        midAlpha = 1.0f
        topAlpha = 1.0f
        evtAlpha = 1.0f
        drawGrid = true
        selectedEvtIdx = None
    }

    resizeRevalidateRepaint()
  }
  // Initialize variables based on selected layer
  selectLayer(selectedLayer)

  //--- BUTTONS ---//
  val layersBtns = enumButtons(MapLayers)(selectedLayer, selectLayer _)
  val toolsBtns = enumButtons(MapViewToolsEnum)(selectedTool, selectedTool = _)

  val undoAction = new Action("Undo") {
    enabled = false

    def refreshEnabled(vs: MapViewState) = {
      enabled = vs.canUndo()
    }

    def apply() = {
      viewStateOpt.map(vs => {
        logger.info("Undo called")
        vs.undo()
        refreshEnabled(vs)
        repaintAll()
      })
    }
  }

  // Defined so we know to update the state of the undo action
  def commitVS(vs: MapViewState) = {
    vs.commit()
    undoAction.refreshEnabled(vs)
  }

  toolbar.contents += new Button(undoAction) {
    val a =
      KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK)

    peer
      .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(a, "UndoAction")
    peer
      .getActionMap.put("UndoAction", undoAction.peer)
  }

  addBtnsAsGrp(toolbar.contents, layersBtns)
  addBtnsAsGrp(toolbar.contents, toolsBtns)

  override lazy val canvasPanel = new MapViewPanel {
    override def paintComponent(g: Graphics2D) =
      {
        super.paintComponent(g)

        viewStateOpt.map(vs => {

          drawWithAlpha(g, evtAlpha) {
            // draw start loc
            val startingLoc = sm.getProjData.startingLoc
            if (startingLoc.map == vs.mapName) {
              import MapEditor.startingLocIcon
              g.drawImage(startingLocIcon,
                (startingLoc.x * curTilesize).toInt - curTilesize / 2,
                (startingLoc.y * curTilesize).toInt - curTilesize / 2,
                curTilesize, curTilesize,
                null, null)
            }
          }
        })
      }
  }

  /**
   * A scroll pane that knows how to store and restore center coords
   */
  override lazy val scrollPane = new CanvasScrollPane(canvasPanel) {
    def storeCenters() = viewStateOpt.map { vs =>
      val viewRect = peer.getViewport().getViewRect()

      val cx = if (viewRect.width > vs.mapMeta.xSize * curTilesize) {
        vs.mapMeta.xSize / 2.0f
      } else {
        viewRect.getCenterX().toFloat / curTilesize
      }

      val cy = if (viewRect.height > vs.mapMeta.ySize * curTilesize) {
        vs.mapMeta.ySize / 2.0f
      } else {
        viewRect.getCenterY().toFloat / curTilesize
      }

      val newMetadata = vs.mapMeta.copy(viewCenterX = cx, viewCenterY = cy)
      //logger.debug("Stored centers as (%f, %f)".format(cx, cy))
      sm.setMap(vs.mapName, vs.map.copy(metadata = newMetadata), false)
    }

    // Add code to saveCenters upon adjustment
    val viewportChangeListener = new ChangeListener() {
      override def stateChanged(e: ChangeEvent) {
        storeCenters()
      }
    }

    peer.getViewport().addChangeListener(viewportChangeListener)
  }

  //--- ADDING WIDGETS ---//
  contents += toolbar
  contents += scrollPane

  //--- MISC FUNCTIONS ---//
  // Updates cursor square, and queues up any appropriate repaints
  def setTilePaintSq(visibleArg: Boolean, x: Float = 0, y: Float = 0) =
    {
      val (xInt, yInt) = (x.toInt, y.toInt)
      def inBounds =
        viewStateOpt.map(_.mapMeta.withinBounds(xInt, yInt)).getOrElse(false)
      val visible = visibleArg && inBounds

      val newCursorSquare = if (visible) {
        val tCodes = tileSelector.selectionBytes
        assert(tCodes.length > 0 && tCodes(0).length > 0, "Selected tiles empty")
        TileRect(xInt, yInt, tCodes(0).length, tCodes.length)
      } else TileRect.empty

      updateCursorSq(newCursorSquare)
    }

  //--- EVENT POPUP MENU ---//
  import MapLayers._
  def editEvent() = viewStateOpt map { vs =>
    val isNewEvent = selectedEvtIdx.isEmpty

    /**
     * Brings up dialog to create new or edit event at the selected event tile
     */
    vs.begin()

    val event = selectedEvtIdx.map { idx =>
      vs.nextMapData.events(idx)
    } getOrElse {
      vs.nextMapData = vs.nextMapData.copy(
        lastGeneratedEventId = vs.nextMapData.lastGeneratedEventId + 1)
      // Need the +0.5f to offset into center of selected tile 
      RpgEvent.blank(
        vs.nextMapData.lastGeneratedEventId,
        canvasPanel.cursorSquare.x1 + 0.5f,
        canvasPanel.cursorSquare.y1 + 0.5f)
    }

    val dialog = new EventDialog(
      projectPanel.mainP.topWin,
      sm,
      vs.mapName,
      event,
      onOk = { e: RpgEvent =>
        if (isNewEvent)
          vs.nextMapData = vs.nextMapData.copy(
            events = vs.nextMapData.events ++ Array(e))
        else
          vs.nextMapData.events.update(selectedEvtIdx.get, e)

        commitVS(vs)
        repaintRegion(TileRect(e.x.toInt, e.y.toInt))
      },
      onCancel = { e: RpgEvent =>
        vs.abort()
      })

    dialog.open()
  }

  def deleteEvent() = viewStateOpt map { vs =>
    selectedEvtIdx map { evtIdx =>
      vs.begin()
      // Deletion of events is expensive, but hopefully, not too common
      val oldEvents = vs.nextMapData.events
      val deletedEvt = oldEvents(evtIdx)
      val newEvents = new Array[RpgEvent](oldEvents.size - 1)
      // Copy over every item except the deleted item
      for (i <- 0 until evtIdx) {
        newEvents.update(i, oldEvents(i))
      }
      for (i <- evtIdx + 1 until oldEvents.size) {
        newEvents.update(i - 1, oldEvents(i))
      }
      vs.nextMapData = vs.nextMapData.copy(events = newEvents)
      commitVS(vs)

      // Repaint deleted event region
      repaintRegion(canvasPanel.cursorSquare)
      // Delete the cached selected event id
      selectedEvtIdx = None
    }
  }

  def showEventPopupMenu(px: Int, py: Int, xTile: Float, yTile: Float) = {
    viewStateOpt map { vs =>
      val evtSelected = selectedEvtIdx.isDefined
      val newEditText = if (evtSelected) "Edit event..." else "New event..."

      val menu = new PopupMenu {
        contents += new MenuItem(Action(newEditText) { editEvent() })

        if (evtSelected)
          contents += new MenuItem(Action("Delete") { deleteEvent() })

        contents += new Separator

        contents += new MenuItem(Action("Set start location") {
          def repaintMapLoc(l: MapLoc) =
            repaintRegion(TileRect(l.x - 0.5f, l.y - 0.5f, 1, 1))

          val oldStartingLoc = sm.getProjData.startingLoc
          val newStartingLoc =
            MapLoc(vs.mapName, xTile.toInt + 0.5f, yTile.toInt + 0.5f)

          sm.setProjData(sm.getProjData.copy(startingLoc = newStartingLoc))

          repaintMapLoc(oldStartingLoc)
          repaintMapLoc(newStartingLoc)
        })
      }

      popupMenuOpen = true
      menu.showWithCallback(canvasPanel, px, py, () => popupMenuOpen = false)
    }
  }

  override def mousePressed(
    e: MousePressed,
    xTile0: Float,
    yTile0: Float,
    vs: MapViewState): Option[(Boolean, MouseFunction, MouseFunction)] = {

    if (!vs.mapMeta.withinBounds(xTile0, yTile0))
      return None

    // Updated the selected event idx
    val existingEventIdx = vs.nextMapData.events.indexWhere(
      e => e.x.toInt == xTile0.toInt && e.y.toInt == yTile0.toInt)

    if (existingEventIdx == -1) {
      selectedEvtIdx = None
    } else {
      selectedEvtIdx = Some(existingEventIdx)
    }

    val button = e.peer.getButton()

    if (selectedLayer == Evt) {
      updateCursorSq(TileRect(xTile0.toInt, yTile0.toInt))

      if (button == MouseEvent.BUTTON1) {
        if (selectedEvtIdx.isDefined) {
          val evtIdx = selectedEvtIdx.get

          vs.begin()

          def onDrag(xTile1: Float, yTile1: Float, vs: MapViewState) = {
            val evt = vs.nextMapData.events(evtIdx)
            vs.nextMapData.events.update(evtIdx,
              evt.copy(x = xTile1.toInt + 0.5f, y = yTile1.toInt + 0.5f))

            updateCursorSq(TileRect(xTile1, yTile1))
          }

          def onDragStop(xTile2: Float, yTile2: Float, vs: MapViewState) = {
            commitVS(vs)
          }

          Some((true, onDrag _, onDragStop _))
        } else None
      } else if (button == MouseEvent.BUTTON3) {
        showEventPopupMenu(e.point.x, e.point.y, xTile0.toInt, yTile0.toInt)
        None
      } else None
    } else {
      if (button == MouseEvent.BUTTON1) {
        vs.begin()

        val tCodes = tileSelector.selectionBytes
        val tool = MapViewToolsEnum.getTool(selectedTool)

        setTilePaintSq(tool.selectionSqOnDrag, xTile0, yTile0)
        val changedRegion =
          tool.onMouseDown(vs, tCodes, selectedLayer, xTile0.toInt, yTile0.toInt)
        repaintRegion(changedRegion)

        def onDrag(xTile1: Float, yTile1: Float, vs: MapViewState) = {
          setTilePaintSq(tool.selectionSqOnDrag, xTile1, yTile1)
          val changedRegion =
            tool.onMouseDragged(vs, tCodes, selectedLayer,
              xTile0.toInt, yTile0.toInt, xTile1.toInt, yTile1.toInt)
          repaintRegion(changedRegion)
        }

        def onDragStop(xTile2: Float, yTile2: Float, vs: MapViewState) = {
          val changedRegion =
            tool.onMouseUp(vs, tCodes, selectedLayer,
              xTile0.toInt, yTile0.toInt, xTile2.toInt, yTile2.toInt)
          repaintRegion(changedRegion)

          commitVS(vs)

          setTilePaintSq(true, xTile2, yTile2)
        }

        Some((true, onDrag _, onDragStop _))
      } else if (button == MouseEvent.BUTTON3) {
        updateCursorSq(TileRect(xTile0.toInt, yTile0.toInt))
        showEventPopupMenu(e.point.x, e.point.y, xTile0, yTile0)
        None
      } else None
    }
  }

  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)

  def cursorFollowsMouse = selectedLayer != Evt && !popupMenuOpen

  reactions += {
    /**
     * Three reactions in the case that the selectedLayer is not the Evt layer
     */
    case MouseMoved(`canvasPanel`, p, _) if cursorFollowsMouse => {
      val (tileX, tileY) = toTileCoords(p)
      setTilePaintSq(true, tileX.toInt, tileY.toInt)
    }
    case MouseExited(`canvasPanel`, _, _) if cursorFollowsMouse =>
      setTilePaintSq(false)
    case e: MouseClicked if e.source == canvasPanel && selectedLayer == Evt =>
      logger.info("MouseClicked on EvtLayer")
      if (e.clicks == 2) {
        editEvent()
      }
  }
}

object MapEditor {
  lazy val startingLocIcon = ImageIO.read(
    getClass.getClassLoader.getResourceAsStream("player_play.png"))
}
