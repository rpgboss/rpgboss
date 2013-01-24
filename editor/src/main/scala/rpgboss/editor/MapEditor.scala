package rpgboss.editor

import rpgboss.lib._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.editor.tileset._
import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._
import com.weiglewilczek.slf4s.Logging
import scala.math._
import scala.swing._
import scala.swing.event._
import javax.imageio._
import java.awt.{BasicStroke, AlphaComposite, Color}
import java.awt.geom.Line2D
import java.awt.event.MouseEvent
import rpgboss.model.event.RpgEvent
import rpgboss.editor.dialog.EventDialog
import java.awt.image.BufferedImage
import scala.collection.mutable.Buffer
import javax.swing.event._

class MapEditor(
    projectPanel: ProjectPanel, 
    sm: StateMaster, 
    tileSelector: TabbedTileSelector)
extends MapView(projectPanel.mainP.topWin, sm, MapScales.scale1)
{
  private var selectedLayer = MapLayers.default
  private var selectedTool = MapViewToolsEnum.default
  private var selectedEvtIdx: Option[Int] = None
  
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
  
  addBtnsAsGrp(toolbar.contents, layersBtns)
  addBtnsAsGrp(toolbar.contents, toolsBtns)
  
  override lazy val canvasPanel = new MapViewPanel {
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      
      viewStateOpt.map(vs => {
        
        drawWithAlpha(g, evtAlpha) {
          // draw start loc
          val startingLoc = sm.getProj.data.startingLoc
          if(startingLoc.map == vs.mapName) {
            import MapEditor.startLocTile
            g.drawImage(startLocTile,
              (startingLoc.x*curTilesize).toInt-curTilesize/2, 
              (startingLoc.y*curTilesize).toInt-curTilesize/2,
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
      
      val cx = if(viewRect.width > vs.mapMeta.xSize*curTilesize) {
        vs.mapMeta.xSize/2.0f
      } else {
        viewRect.getCenterX().toFloat/curTilesize
      }
      
      val cy = if(viewRect.height > vs.mapMeta.ySize*curTilesize) {
        vs.mapMeta.ySize/2.0f
      } else {
        viewRect.getCenterY().toFloat/curTilesize
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
    
    val newCursorSquare = if(visible) {
      val tCodes = tileSelector.selectedTileCodes 
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
          canvasPanel.cursorSquare.x1+0.5f, 
          canvasPanel.cursorSquare.y1+0.5f)
    }
    
    val dialog = new EventDialog(
        projectPanel.mainP.topWin, 
        sm,
        vs.mapName,
        event,
        onOk = { e: RpgEvent =>
          if(isNewEvent) 
            vs.nextMapData = vs.nextMapData.copy(
                events = vs.nextMapData.events ++ Array(e))
          else
            vs.nextMapData.events.update(selectedEvtIdx.get, e)
          
          vs.commit()
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
      val newEvents = new Array[RpgEvent](oldEvents.size-1)
      // Copy over every item except the deleted item
      for(i <- 0 until evtIdx) {
        newEvents.update(i, oldEvents(i))
      }
      for(i <- evtIdx+1 until oldEvents.size) {
        newEvents.update(i-1, oldEvents(i))
      }
      vs.nextMapData = vs.nextMapData.copy(events = newEvents)
      vs.commit()

      // Repaint deleted event region
      repaintRegion(canvasPanel.cursorSquare)
      // Delete the cached selected event id
      selectedEvtIdx = None
    }
  }
  
  def evtPopupMenu(): Option[PopupMenu] = viewStateOpt map { vs =>
    val isNewEvent = selectedEvtIdx.isEmpty
    val newEditText = if(isNewEvent) "New event" else "Edit event"
    
    val menu = new PopupMenu {
      contents += new MenuItem(Action(newEditText) { editEvent() })
      contents += new MenuItem(Action("Delete") { deleteEvent() })
    }
    
    menu
  }
  
  override def mousePressed(
      e: MousePressed,
      x0: Float, 
      y0: Float, 
      vs: MapViewState) : Option[(Boolean, MouseFunction, MouseFunction)] = {
    
    if(selectedLayer == Evt) {
      if(vs.mapMeta.withinBounds(x0.toInt, y0.toInt)) {
        updateCursorSq(TileRect(x0.toInt, y0.toInt))
        
        // Updated the selected event idx
        val existingEventIdx = 
          vs.nextMapData.events.indexWhere(e => 
            e.x.toInt == x0.toInt && e.y.toInt == y0.toInt)
      
        if(existingEventIdx == -1) {
          selectedEvtIdx = None
        } else {
          selectedEvtIdx = Some(existingEventIdx)
        }
        
        val button = e.peer.getButton()
        if(button == MouseEvent.BUTTON1) {
          if(selectedEvtIdx.isDefined) {
            val evtIdx = selectedEvtIdx.get
            
            vs.begin()
            
            def onDrag(x1: Float, y1: Float, vs: MapViewState) = {
              val evt = vs.nextMapData.events(evtIdx)
              vs.nextMapData.events.update(evtIdx, 
                  evt.copy(x = x1.toInt+0.5f, y = y1.toInt+0.5f))
              
              updateCursorSq(TileRect(x1.toInt, y1.toInt))
            }
            
            def onDragStop(x2: Float, y2: Float, vs: MapViewState) = {
              vs.commit()
            }
            
            Some((true, onDrag _, onDragStop _))
          } else None
        } else if(button == MouseEvent.BUTTON3) {
          evtPopupMenu().map { _.show(canvasPanel, e.point.x, e.point.y) }
          None
        } else None
      } else None
    } else {
      vs.begin()
        
      val tCodes = tileSelector.selectedTileCodes
      val tool = MapViewToolsEnum.getTool(selectedTool)
      
      setTilePaintSq(tool.selectionSqOnDrag, x0, y0)
      val changedRegion =
        tool.onMouseDown(vs, tCodes, selectedLayer, x0.toInt, y0.toInt)
      repaintRegion(changedRegion)
      
      def onDrag(x1: Float, y1: Float, vs: MapViewState) = {
        setTilePaintSq(tool.selectionSqOnDrag, x1, y1)
        val changedRegion =
          tool.onMouseDragged(vs, tCodes, selectedLayer, 
                x0.toInt, y0.toInt, x1.toInt, y1.toInt)
        repaintRegion(changedRegion)
      }
      
      def onDragStop(x2: Float, y2: Float, vs: MapViewState) = {
        val changedRegion =
          tool.onMouseUp(vs, tCodes, selectedLayer, 
                x0.toInt, y0.toInt, x2.toInt, y2.toInt)
        repaintRegion(changedRegion)
        
        vs.commit()
            
        setTilePaintSq(true, x2, y2)
      }
      
      Some((true, onDrag _, onDragStop _))
    }
  }
  
  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  reactions += {
    /**
     * Three reactions in the case that the selectedLayer is not the Evt layer
     */
    case MouseMoved(`canvasPanel`, p, _) if selectedLayer != Evt => {
      val (tileX, tileY) = toTileCoords(p)
      setTilePaintSq(true, tileX.toInt, tileY.toInt)
    }
    case MouseExited(`canvasPanel`, _, _) if selectedLayer != Evt =>
      setTilePaintSq(false)
    case e: MouseClicked if e.source == canvasPanel && selectedLayer == Evt =>
      logger.info("MouseClicked on EvtLayer")
      if(e.clicks == 2) {
        editEvent()
      }
  }
}

object MapEditor {
  lazy val startLocTile = ImageIO.read(
    getClass.getClassLoader.getResourceAsStream("player_play.png"))
}
