package rpgboss.editor.lib

import rpgboss.lib._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.tileset._
import rpgboss.editor._
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

object MapScales extends RpgEnum {
  val scale1 = Value(1, "1/1")
  val scale2 = Value(2, "1/2")
  val scale4 = Value(4, "1/4")
  
  def default = scale1
}

class MapView(
    owner: Window, 
    sm: StateMaster, 
    initialScale: MapScales.Value)
extends BoxPanel(Orientation.Vertical) with SelectsMap with Logging
{
  //--- VARIABLES ---//
  protected var viewStateOpt : Option[MapViewState] = None
  protected var curTilesize = Tileset.tilesize/initialScale.id
  protected var botAlpha = 1.0f
  protected var midAlpha = 1.0f
  protected var topAlpha = 1.0f
  protected var evtAlpha = 1.0f
  protected var drawGrid = true
  
  //--- EVT IMG CACHE ---//
  val evtImgCache = new EventImageCache(sm)
  
  val scaleButtons = enumButtons(MapScales)(
      initialScale, 
      s => {
        curTilesize = Tileset.tilesize/s.id
        resizeRevalidateRepaint()
      })
  
  //--- WIDGETS --//
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    minimumSize = new Dimension(200, 32)
    
    addBtnsAsGrp(contents, scaleButtons)
    
    contents += Swing.HGlue
  }
  
  class MapViewPanel extends Panel {
    var cursorSquare: TileRect = TileRect.empty
    
    background = Color.WHITE
    
    def drawWithAlpha(g: Graphics2D, alpha: Float)(drawCmd: => Any) = {
      if(alpha > 0.0f) {
        if(alpha == 1.0f)
          g.setComposite(AlphaComposite.SrcOver)
        else
          g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, alpha))
        
        drawCmd
      }
    }
    
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      
      viewStateOpt.map(vs => {
        
        val bounds = g.getClipBounds
        
        val (minX, minY, maxX, maxY, minXTile, minYTile, maxXTile, maxYTile) =
          TileUtils.getTileBounds(
              g.getClipBounds(), curTilesize, curTilesize, 
              vs.mapMeta.xSize, vs.mapMeta.ySize)
        
        //logger.info("Paint Tiles: x: [%d,%d], y: [%d,%d]".format(
        //  minXTile, maxXTile, minYTile, maxYTile))
          
        // draw tiles
        import MapLayers._
        val alphaList = List(botAlpha, midAlpha, topAlpha)
        val alphasAndData = vs.nextMapData.drawOrder zip alphaList
        MapLayers.drawOrderZip(alphasAndData).map {
          case(curLayer, (layerAry, alpha)) => drawWithAlpha(g, alpha) {
            
            for(xTile <- minXTile to maxXTile; yTile <- minYTile to maxYTile) {
              if(layerAry(yTile)(xTile*RpgMap.bytesPerTile) != -1) {
                val tileImg = 
                  vs.tileCache.getTileImage(layerAry, xTile, yTile, 0)
                
                g.drawImage(tileImg, 
                            xTile*curTilesize, yTile*curTilesize,
                            (xTile+1)*curTilesize, (yTile+1)*curTilesize,
                            0, 0, Tileset.tilesize, Tileset.tilesize,
                            null)
              }
            }  
          }
        }
        
        // draw all other events
        val evtFill = new Color(0.5f, 0.5f, 0.5f, 0.5f)
        drawWithAlpha(g, evtAlpha) {
          
          g.setStroke(new BasicStroke())
          vs.nextMapData.events.foreach( rpgevt =>
            // If within view frame
            if( rpgevt.x + 0.5 > minX && rpgevt.x - 0.5 < maxX &&
                rpgevt.y + 0.5 > minY && rpgevt.y - 0.5 < maxY) {
              g.setColor(Color.WHITE)
              g.drawRect(
                  ((rpgevt.x-0.5)*curTilesize+2).toInt,
                  ((rpgevt.y-0.5)*curTilesize+2).toInt,
                  curTilesize-4-1,
                  curTilesize-4-1)
              
              val dx1 = ((rpgevt.x-0.5)*curTilesize+2).toInt+1
              val dy1 = ((rpgevt.y-0.5)*curTilesize+2).toInt+1
              rpgevt.states.head.sprite.map { spriteSpec =>
                g.drawImage(
                    evtImgCache.get(spriteSpec), 
                    dx1, dy1, curTilesize-4-1, curTilesize-4-1, evtFill, null)
              } getOrElse {
                g.setColor(evtFill)
                g.fillRect(dx1, dy1, curTilesize-4-1, curTilesize-4-1)
              }
              
            }
          )
        }
        
        if(drawGrid) {
          // draw grid if on evt layer
          TileUtils.drawGrid(
              g, curTilesize, curTilesize, 
              minXTile, minYTile, maxXTile, maxYTile)
        }
        
        cursorSquare.optionallyDrawSelRect(g, curTilesize, curTilesize)
      })
    }
  }
  
  lazy val canvasPanel = new MapViewPanel()
  
  /**
   * A scroll pane that knows how to store and restore center coords
   */
  class CanvasScrollPane(canvas: MapViewPanel) extends ScrollPane(canvas) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Always
    verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
    
    // We do this on the EDT, because the viewport size does not seem to be
    // available until after the scrollpane is rendered for the first time
    def center(cx: Float, cy: Float) = Swing.onEDT {
      val viewport = peer.getViewport()
      val viewportSize = viewport.getExtentSize()
      val viewportW = viewportSize.getWidth().toFloat
      val viewportH = viewportSize.getHeight().toFloat
      
      // Calculate new origins based on the stored desired centers
      val viewOrigX = max(0f, cx*curTilesize - viewportW/2)
      val viewOrigY = max(0f, cy*curTilesize - viewportH/2)
      
      viewport.setViewPosition(new Point(viewOrigX.toInt, viewOrigY.toInt))
    }
    
    def restoreCenters() = viewStateOpt.map { vs =>
      center(vs.mapMeta.viewCenterX, vs.mapMeta.viewCenterY)
    }
  }
  
  lazy val scrollPane = new CanvasScrollPane(canvasPanel)
  
  //--- ADDING WIDGETS ---//
  contents += toolbar
  contents += scrollPane
  
  //--- MISC FUNCTIONS ---//
  def toTileCoords(p: Point) : (Float, Float) = 
    (p.getX.toFloat/curTilesize, p.getY.toFloat/curTilesize) 
  
  def resizeRevalidateRepaint() = {
    canvasPanel.preferredSize = viewStateOpt.map { vs =>
      new Dimension(vs.mapMeta.xSize*curTilesize,
                    vs.mapMeta.ySize*curTilesize)
    } getOrElse {
      new Dimension(0,0)
    }
    
    println(canvasPanel.preferredSize)
    
    canvasPanel.revalidate()
    canvasPanel.repaint()
    
    scrollPane.revalidate()
  }
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    viewStateOpt = mapOpt map { mapMeta =>
      new MapViewState(sm, mapMeta.name)
    }
      
    resizeRevalidateRepaint()
    
    // Restore centers upon loading a new map
    scrollPane.restoreCenters()
  }
  
  // Updates cursor square, and queues up any appropriate repaints
  def updateCursorSq(newTileRect: TileRect) = 
  {
    val oldSq = canvasPanel.cursorSquare
    canvasPanel.cursorSquare = newTileRect
    
    // if updated, redraw. otherwise, don't redraw
    if(oldSq != canvasPanel.cursorSquare) {
      repaintRegion(oldSq)
      repaintRegion(canvasPanel.cursorSquare)
    }
  }
  
  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  def repaintRegion(r1: TileRect) =
    canvasPanel.repaint(r1.rect(curTilesize, curTilesize))
  def repaintAll() = canvasPanel.repaint()
  
  type MouseFunction = 
    (Float, Float, MapViewState) => Any
  
  /**
   * Callback for when the mouse is pressed. Should return additional callbacks
   * to be used upon drag and end-drag action
   * @return (onlyCallOnTileChange, dragCallback, dragStopCallback)
   * 
   * onlyCallOnTileChange = only call the dragCallback if the tile is different
   */
  def mousePressed(
      e: MousePressed,
      x0: Float, 
      y0: Float, 
      vs: MapViewState) : Option[(Boolean, MouseFunction, MouseFunction)] = {
    updateCursorSq(TileRect(x0 - 0.5f, y0-0.5f, 1, 1))
    
    // Treat drags and drag stops as also mousePressed
    Some((true, mousePressed(e, _, _, _), mousePressed(e, _, _, _)))
  }
    
  reactions += {
    case e: MousePressed if e.source == canvasPanel => 
      viewStateOpt map { vs => 
        val (x0, y0) = toTileCoords(e.point)
        
        val dragAndDragStopActionsOpt = 
          mousePressed(e, x0, y0, vs)
        
        if(dragAndDragStopActionsOpt.isDefined) {
          val (onlyCallOnTileChange, dragAction, dragStopAction) = 
            dragAndDragStopActionsOpt.get
            
          // init to impossible value to ensure first drag action always called
          var (xLastDragTile, yLastDragTile) = (-1, -1)
          
          lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
            case e: MouseDragged if e.source == canvasPanel => {
              val (x1, y1) = toTileCoords(e.point)
              val x1Tile = x1.toInt
              val y1Tile = y1.toInt
              
              // only redo action if dragged to a different square
              if( !onlyCallOnTileChange || 
                  (x1Tile, y1Tile) != (xLastDragTile, yLastDragTile) ) {
                dragAction(x1, y1, vs)
                
                xLastDragTile = x1Tile
                yLastDragTile = y1Tile
              }
            }
            case e: MouseReleased if e.source == canvasPanel => {
              val (x2, y2) = toTileCoords(e.point)
              
              dragStopAction(x2, y2, vs)
              reactions -= temporaryReactions
            }
          }
          
          reactions += temporaryReactions
        }
      }
  }
}