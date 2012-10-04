package rpgboss.editor

import rpgboss.lib._
import rpgboss.cache._
import rpgboss.model._
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

class MapView(
    projectPanel: ProjectPanel, 
    sm: StateMaster, 
    tileSelector: TabbedTileSelector)
extends BoxPanel(Orientation.Vertical) with SelectsMap with Logging
{
  //--- VARIABLES ---//
  var viewStateOpt : Option[MapViewState] = None
  var curTilesize = Tileset.tilesize
  
  //--- CONVENIENCE DEFS ---//
  def selectedLayer = MapLayers.selected
  
  //--- BUTTONS ---//
  def scaleButton(title: String, invScale: Int) = new RadioButton() { 
    action = Action(title) {
      curTilesize = Tileset.tilesize / invScale
      resizeRevalidateRepaint()
    }
  }
  
  val s11Btn = scaleButton("1/1", 1)
  val s12Btn = scaleButton("1/2", 2)
  val s14Btn = scaleButton("1/4", 4)
  
  //--- WIDGETS --//
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    minimumSize = new Dimension(200, 32)
    def addBtnsAsGrp(btns: List[RadioButton]) = {
      val grp = new ButtonGroup(btns : _*)
      grp.select(btns.head)
      
      contents ++= btns
    }
    
    def enumButtons[T](enum: ListedEnum[T]) = 
      enum.valueList.map { eVal =>
        new RadioButton() {
          action = Action(eVal.toString) { 
            enum.selected = eVal 
            resizeRevalidateRepaint()
          }
        }
      }
    
    addBtnsAsGrp(enumButtons(MapLayers))
    addBtnsAsGrp(enumButtons(MapViewTools))
    addBtnsAsGrp(List(s11Btn, s12Btn, s14Btn))
    
    contents += Swing.HGlue
  }
  
  val canvasPanel = new Panel() {
    var cursorSquare : TileRect = TileRect.empty
    var eventSelection : TileRect = TileRect.empty
    
    background = Color.WHITE
    
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      
      viewStateOpt.map(vs => {
        
        val bounds = g.getClipBounds
        val tilesize = curTilesize
        
        val minXTile = (bounds.getMinX/tilesize).toInt
        val minYTile = (bounds.getMinY/tilesize).toInt
        
        val maxXTile = 
          min(vs.mapMeta.xSize-1, (bounds.getMaxX.toInt-1)/tilesize)
        val maxYTile = 
          min(vs.mapMeta.ySize-1, (bounds.getMaxY.toInt-1)/tilesize)
        
        /*logger.info("Paint Tiles: x: [%d,%d], y: [%d,%d]".format(
          minXTile, maxXTile, minYTile, maxYTile))*/
          
        // draw tiles
        import MapLayers._
        enumDrawOrder(vs.nextMapData).map {
          case(curLayer, layerAry) => 
            if(selectedLayer != Evt && selectedLayer != curLayer)
              g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.5f))
            else
              g.setComposite(AlphaComposite.SrcOver)
            
            for(xTile <- minXTile to maxXTile; yTile <- minYTile to maxYTile) {
              if(layerAry(yTile)(xTile*RpgMap.bytesPerTile) != -1) {
                val tileImg = 
                  vs.tilecache.getTileImage(layerAry, xTile, yTile, 0)
                
                g.drawImage(tileImg, 
                            xTile*tilesize, yTile*tilesize,
                            (xTile+1)*tilesize, (yTile+1)*tilesize,
                            0, 0, Tileset.tilesize, Tileset.tilesize,
                            null)
              }
            }   
        }
        
        if(selectedLayer == Evt) {
          // draw grid if on evt layer
          g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 0.5f))
          g.setStroke(new BasicStroke(2.0f))
          for(xTile <- minXTile to maxXTile+1) {
            g.draw(new Line2D.Double(xTile*tilesize, minYTile*tilesize,
                                     xTile*tilesize, (maxYTile+1)*tilesize))
          }
          for(yTile <- minYTile to maxYTile+1) {
            g.draw(new Line2D.Double(minXTile*tilesize, yTile*tilesize,
                                     (maxXTile+1)*tilesize, yTile*tilesize))
          }
          
          // draw selection square
          eventSelection.optionallyDrawSelRect(g, tilesize, tilesize)
          
          // draw start loc
          val startingLoc = sm.getProj.data.startingLoc
          if(startingLoc.map == vs.mapName &&
             startingLoc.x >= minXTile && startingLoc.x <= maxXTile &&
             startingLoc.y >= minYTile && startingLoc.y <= maxYTile) {
            g.setComposite(AlphaComposite.SrcOver)
            g.drawImage(MapView.startLocTile,
              (startingLoc.x*tilesize).toInt-MapView.startLocTile.getWidth()/2, 
              (startingLoc.y*tilesize).toInt-MapView.startLocTile.getHeight()/2, 
              null, null) 
          }
        } else {        
          // otherwise draw selection square
          cursorSquare.optionallyDrawSelRect(g, curTilesize, curTilesize)
        }
      })
    }
  }
  
  //--- ADDING WIDGETS ---//
  contents += toolbar
  contents += new ScrollPane(canvasPanel) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Always
    verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  }
  
  //--- MISC FUNCTIONS ---//
  def toTileCoords(p: Point) = 
    (p.getX.toInt/curTilesize, p.getY.toInt/curTilesize) 
  
  def resizeRevalidateRepaint() = {
    canvasPanel.preferredSize = viewStateOpt.map { vs =>
      new Dimension(vs.mapMeta.xSize*curTilesize,
                    vs.mapMeta.ySize*curTilesize)
    } getOrElse {
      new Dimension(0,0)
    }
    
    canvasPanel.revalidate()
    canvasPanel.repaint()
  }
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    viewStateOpt = mapOpt map { mapMeta =>
      val tc = new TileCache(sm.getProj, sm.getAutotiles, mapMeta)
      new MapViewState(sm, mapMeta.name, tc)
    }
      
    resizeRevalidateRepaint()
  }
  
  // Updates cursor square, and queues up any appropriate repaints
  def updateCursorSq(visible: Boolean, x: Int = 0, y: Int = 0) = 
  {
    val oldSq = canvasPanel.cursorSquare
    canvasPanel.cursorSquare = if(visible) {
      val tCodes = tileSelector.selectedTileCodes 
      assert(tCodes.length > 0 && tCodes(0).length > 0, "Selected tiles empty")
      TileRect(x, y, tCodes(0).length, tCodes.length)
    } else TileRect.empty
    
    // if updated, redraw. otherwise, don't redraw
    if(oldSq != canvasPanel.cursorSquare) {
      repaintRegion(oldSq)
      repaintRegion(canvasPanel.cursorSquare)
    }
  }
  
  //--- EVENT POPUP MENU ---//
  var evtPopupMenu: Option[PopupMenu] = None
  def makePopupMenu(x: Int, y: Int) : Option[PopupMenu] = {
    None
  }
  
  /**
   * Brings up dialog to create new or edit event at the selected event tile
   */
  import MapLayers._
  def newOrEditEvent() = viewStateOpt map { vs =>
    if(selectedLayer == Evt && !canvasPanel.eventSelection.empty) {
      val selectedLoc = canvasPanel.eventSelection
      val existingEventIdx = 
        vs.nextMapData.events.indexWhere(e => 
          e.x == selectedLoc.x1 && e.y == selectedLoc.y1)
      
      val isNewEvent = existingEventIdx == -1
      
      vs.begin()
      
      val event = if(isNewEvent) {
        vs.nextMapData = vs.nextMapData.copy(
            lastGeneratedEventId = vs.nextMapData.lastGeneratedEventId + 1)
        RpgEvent.blank(
            vs.nextMapData.lastGeneratedEventId, selectedLoc.x1, selectedLoc.y1)
      } else {
        vs.nextMapData.events(existingEventIdx)
      }
      
      val dialog = new EventDialog(
          projectPanel.mainP.topWin, 
          event,
          onOk = { e: RpgEvent =>
            if(isNewEvent) 
              vs.nextMapData = vs.nextMapData.copy(
                  events = vs.nextMapData.events ++ Array(e))
            else
              vs.nextMapData.events.update(existingEventIdx, e)
            
            vs.commit()
          },
          onCancel = { e: RpgEvent =>
            vs.abort()
          })
      
      dialog.open()
    }
  }
  
  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  def repaintRegion(r1: TileRect) =
    canvasPanel.repaint(r1.rect(curTilesize, curTilesize))
  
  reactions += {
    /**
     * Three reactions in the case that the selectedLayer is not the Evt layer
     */
    case MouseMoved(`canvasPanel`, p, _) if selectedLayer != Evt => {
      val (tileX, tileY) = toTileCoords(p)
      updateCursorSq(true, tileX, tileY)
    }
    case MouseExited(`canvasPanel`, _, _) if selectedLayer != Evt =>
      updateCursorSq(false)
    case MousePressed(`canvasPanel`, point, _, _, _) if selectedLayer != Evt => 
      viewStateOpt map { vs => 
        vs.begin()
        
        val tCodes = tileSelector.selectedTileCodes
        val tool = MapViewTools.selected
        val (x1, y1) = toTileCoords(point)
                
        updateCursorSq(tool.selectionSqOnDrag, x1, y1)
        repaintRegion(MapViewTools.selected.onMousePressed(vs, tCodes, x1, y1))
        
        var (xLastDrag, yLastDrag) = (-1, -1) // init to impossible value
          
        lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
          case MouseDragged(`canvasPanel`, point, _) => {
            val (x2, y2) = toTileCoords(point)
            
            // only redo action if dragged to a different square
            if( (x2, y2) != (xLastDrag, yLastDrag) ) {
              updateCursorSq(tool.selectionSqOnDrag, x2, y2)
              repaintRegion(MapViewTools.selected.onMouseDragged(
                  vs, tCodes, x1, y1, x2, y2))
              
              xLastDrag = x2
              yLastDrag = y2
            }
          }
          case MouseReleased(`canvasPanel`, point, _, _, _) => {
            val (x2, y2) = toTileCoords(point)
            vs.commit()
            
            updateCursorSq(true, x2, y2)
            
            reactions -= temporaryReactions
          }
        }
        
        reactions += temporaryReactions
      }
    case e: MousePressed if e.source == canvasPanel && selectedLayer == Evt => 
      viewStateOpt map { vs => 
        val (x1, y1) = toTileCoords(e.point)
        
        val oldEvtSelection = canvasPanel.eventSelection
        canvasPanel.eventSelection = TileRect(x1, y1)
        repaintRegion(oldEvtSelection)
        repaintRegion(canvasPanel.eventSelection)
        
        // Update the stored popup menu
        evtPopupMenu = makePopupMenu(x1, y1)
        
        if(e.peer.getButton() == MouseEvent.BUTTON3) {
          
        }
      }
  }
}

object MapView {
  lazy val startLocTile = ImageIO.read(
    getClass.getClassLoader.getResourceAsStream("player_play.png"))
}
