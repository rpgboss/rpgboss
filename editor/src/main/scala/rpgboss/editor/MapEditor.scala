package rpgboss.editor

import rpgboss.lib._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.editor.resourceselector._
import rpgboss.editor.uibase._
import rpgboss.editor.misc._
import rpgboss.editor.misc.GraphicsUtils._
import com.typesafe.scalalogging.slf4j.LazyLogging
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
import javax.swing.ImageIcon
import rpgboss.editor.dialog.EventInstanceDialog
import rpgboss.editor.Internationalized._

class MapEditor(
  projectPanel: ProjectPanel,
  sm: StateMaster,
  tileSelector: TabbedTileSelector)
  extends MapView(projectPanel.mainP.topWin, sm, MapScales.scale1) {
  private var selectedLayer = MapLayers.default
  private var selectedTool = MapViewToolsEnum.default
  private var selectedEvtId: Option[Int] = None
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
        selectedEvtId = None
    }

    resizeRevalidateRepaint()
  }
  // Initialize variables based on selected layer
  selectLayer(selectedLayer)

  // Defined so we know to update the state of the undo action
  def commitVS(vs: MapViewState) = {
    vs.commit()
    undoButton.refreshEnabled(vs)
  }

  //--- BUTTONS ---//

  // Clear out scale buttons
  toolbar.contents.clear()

  val layerButtons = enumButtons(MapLayers)(
      selectedLayer,
      selectLayer,
      List(
          "hendrik-weiler-theme/bottom-layer.png",
          "hendrik-weiler-theme/middle-layer.png",
          "hendrik-weiler-theme/top-layer.png",
          "hendrik-weiler-theme/event-layer.png"))

  addBtnsAsGrp(toolbar.contents, layerButtons)

  toolbar.contents += Swing.HStrut(16)

  val toolsButtons = enumButtons(MapViewToolsEnum)(
      selectedTool,
      selectedTool = _,
      iconPaths = List(
          "hendrik-weiler-theme/pencil.png",
          "hendrik-weiler-theme/rectangle.png",
          "hendrik-weiler-theme/circle.png",
          "hendrik-weiler-theme/bucket.png",
          "hendrik-weiler-theme/eraser.png"))

  addBtnsAsGrp(toolbar.contents, toolsButtons)

  toolbar.contents += Swing.HStrut(16)

  val undoButton = new Button() {
    def refreshEnabled(vs: MapViewState) = {
      action.enabled = vs.canUndo()
    }

    action = new Action(getMessage("Undo")) {
      enabled = false

      def apply() = {
        viewStateOpt.map(vs => {
          logger.info(getMessage("Undo_Called"))
          vs.undo()
          refreshEnabled(vs)
          repaintAll()
        })
      }
    }

    peer
      .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK), getMessage("Undo"))
    peer
      .getActionMap.put(getMessage("Undo"), action.peer)

    icon = new ImageIcon(Utils.readClasspathImage(
        "hendrik-weiler-theme/undo.png"))
  }

  toolbar.contents += undoButton

  toolbar.contents += Swing.HStrut(16)

  // Add back the scale buttons
  addBtnsAsGrp(toolbar.contents, scaleButtons)

  toolbar.contents += Swing.HGlue

  override lazy val canvasPanel = new MapViewPanel {
    override def paintComponent(g: Graphics2D) =
      {
        super.paintComponent(g)

        viewStateOpt.map(vs => {

          drawWithAlpha(g, evtAlpha) {
            // draw start loc
            val startingLoc = sm.getProjData.startup.startingLoc
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

      val newMetadata = vs.mapMeta.copy(editorCenterX = cx, editorCenterY = cy)
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
        assert(tCodes.length > 0 && tCodes(0).length > 0, getMessage("Selected_Tiles_Empty"))
        TileRect(xInt, yInt, tCodes(0).length, tCodes.length)
      } else TileRect.empty

      updateCursorSq(newCursorSquare)
    }

  //--- EVENT POPUP MENU ---//
  import MapLayers._

  def newEvent(eventInstance: Boolean) = viewStateOpt map { vs =>
    val id = vs.mapMeta.lastGeneratedEventId + 1
    val x = canvasPanel.cursorSquare.x1 + 0.5f
    val y = canvasPanel.cursorSquare.y1 + 0.5f

    val event = if (eventInstance)
      RpgEvent.blankInstance(id, x, y)
    else
      RpgEvent.blank(id, x, y)

    showEditDialog(true, vs, event)
  }

  def editEvent(id: Int) = viewStateOpt map { vs =>
    val event = vs.nextMapData.events(id)
    showEditDialog(false, vs, event)
  }

  def showEditDialog(isNewEvent: Boolean, vs: MapViewState, event: RpgEvent) = {
    vs.begin()

    def onOk(e: RpgEvent) = {
      if (isNewEvent) {
        val newMetadata = vs.mapMeta.copy(
          lastGeneratedEventId = vs.mapMeta.lastGeneratedEventId + 1)
        sm.setMap(vs.mapName, vs.map.copy(metadata = newMetadata))
      }
      vs.nextMapData.events = vs.nextMapData.events.updated(e.id, e)

      commitVS(vs)
      repaintRegion(TileRect(e.x.toInt, e.y.toInt))
    }

    def onCancel(e: RpgEvent) =
      vs.abort()

    val dialog = if (event.isInstance) {
      new EventInstanceDialog(
        projectPanel.mainP.topWin,
        sm,
        event,
        onOk = onOk,
        onCancel = onCancel)
    } else {
      new EventDialog(
        projectPanel.mainP.topWin,
        sm,
        vs.mapName,
        event,
        onOk = onOk,
        onCancel = onCancel)
    }

    dialog.open()
  }

  def deleteEvent() = viewStateOpt map { vs =>
    selectedEvtId map { id =>
      vs.begin()
      vs.nextMapData.events = vs.nextMapData.events - id
      commitVS(vs)

      // Repaint deleted event region
      repaintRegion(canvasPanel.cursorSquare)
      // Delete the cached selected event id
      selectedEvtId = None
    }
  }

  var eventIsCopied = false

  def copyEvent() = {
    eventIsCopied = true
    logger.info("Event copied.")
  }

  def pasteEvent() = {
    logger.info("Event pasted.")
  }

  val actionCopyEvent = Action(getMessage("Copy_Event")) {
    copyEvent()
  }

  val actionPasteEvent = Action(getMessage("Paste_Event")) {
    pasteEvent()
  }

  peer
    .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), getMessage("Copy_Event"))
  peer
    .getActionMap.put(getMessage("Copy_Event"), actionCopyEvent.peer)

  peer
    .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), getMessage("Paste_Event"))
  peer
    .getActionMap.put(getMessage("Paste_Event"), actionPasteEvent.peer)

  def showEventPopupMenu(px: Int, py: Int, xTile: Float, yTile: Float) = {
    viewStateOpt map { vs =>
      val evtSelected = selectedEvtId.isDefined
      val newEditText = if (evtSelected) getMessage("Edit_Event") + "..." else getMessage("New_Event") + "..."

      val menu = new RpgPopupMenu {
        if (evtSelected) {
          contents += new MenuItem(Action(getMessage("Edit") + "...") {
            editEvent(selectedEvtId.get)
          })
        } else {
          contents += new MenuItem(Action(getMessage("New_Event") + "...") { newEvent(false) })
          contents += new MenuItem(
              Action(getMessage("New_Event_Instance") + "...") { newEvent(true) })
        }

        if (evtSelected) {
          contents += new MenuItem(Action(getMessage("Delete")) { deleteEvent() })
          contents += new MenuItem(Action(getMessage("Copy_Event")) { copyEvent() })
          if(eventIsCopied) {
            contents += new MenuItem(Action(getMessage("Paste_Event")) { pasteEvent() })
          }
        }

        contents += new Separator

        contents += new MenuItem(Action(getMessage("Set_Start_Location")) {
          def repaintMapLoc(l: MapLoc) =
            repaintRegion(TileRect(l.x - 0.5f, l.y - 0.5f, 1, 1))

          val projData = sm.getProjData
          val oldStartingLoc = sm.getProjData.startup.startingLoc
          val newStartingLoc =
            MapLoc(vs.mapName, xTile.toInt + 0.5f, yTile.toInt + 0.5f)

          projData.startup.startingLoc = newStartingLoc
          sm.setProjData(projData)

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

    // Updated the selected event id
    selectedEvtId = vs.nextMapData.events.find {
      case (id, event) =>
        event.x.toInt == xTile0.toInt && event.y.toInt == yTile0.toInt
    }.map(_._1)

    val button = e.peer.getButton()

    if (selectedLayer == Evt) {
      updateCursorSq(TileRect(xTile0.toInt, yTile0.toInt))

      if (button == MouseEvent.BUTTON1) {
        if (selectedEvtId.isDefined) {
          vs.begin()

          def onDrag(xTile1: Float, yTile1: Float, vs: MapViewState) = {
            val evt = vs.nextMapData.events(selectedEvtId.get)
            evt.x = xTile1.toInt + 0.5f
            evt.y = yTile1.toInt + 0.5f
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
        selectedEvtId
          .map(id => editEvent(id))
          .getOrElse(newEvent(eventInstance = false))
      }
  }
}

object MapEditor {
  lazy val startingLocIcon = rpgboss.lib.Utils.readClasspathImage("player_play.png")
}
