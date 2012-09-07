package rpgboss.editor

import rpgboss.lib._
import rpgboss.cache._
import rpgboss.model._

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

class MapView(sm: StateMaster, tileSelector: TabbedTileSelector)
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
    var cursorSquare : TileRect = TileRect()
    
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
          
          // draw start loc
          val startingLoc = sm.getProj.data.startingLoc
          if(startingLoc.map == vs.mapId &&
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
      new MapViewState(sm, mapMeta.id, tc)
    }
      
    resizeRevalidateRepaint()
  }
  
  // Returns TileRect to redraw
  def updateCursorSq(visible: Boolean, x: Int = 0, y: Int = 0) : TileRect = 
  {
    val oldSq = canvasPanel.cursorSquare
    val newSq = if(visible) {
      val tCodes = tileSelector.selectedTileCodes 
      assert(tCodes.length > 0 && tCodes(0).length > 0, "Selected tiles empty")
      TileRect(x, y, tCodes(0).length, tCodes.length)
    } else TileRect()
    
    // if updated, redraw. otherwise, don't redraw
    if(oldSq != newSq) {
      canvasPanel.cursorSquare = newSq
      oldSq|newSq
    } else TileRect()
  }
  
  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  def repaintRegions(r1: TileRect, r2: TileRect = TileRect()) =
    canvasPanel.repaint((r1|r2).rect(curTilesize, curTilesize))
  
  import MapLayers._
  reactions += {
    case MouseMoved(`canvasPanel`, p, _) if selectedLayer != Evt => {
      val (tileX, tileY) = toTileCoords(p)
      repaintRegions(updateCursorSq(true, tileX, tileY))
    }
    case MouseExited(`canvasPanel`, _, _) if selectedLayer != Evt=>
      repaintRegions(updateCursorSq(false))
    case MousePressed(`canvasPanel`, point, _, _, _) 
    if selectedLayer != Evt => {
      viewStateOpt map { vs => 
        val tCodes = tileSelector.selectedTileCodes
        val tool = MapViewTools.selected
        val (x1, y1) = toTileCoords(point)
                
        repaintRegions(
          updateCursorSq(tool.selectionSqOnDrag, x1, y1),
          MapViewTools.selected.onMousePressed(vs, tCodes, x1, y1))
        
        var (xLastDrag, yLastDrag) = (-1, -1) // init to impossible value
          
        lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
          case MouseDragged(`canvasPanel`, point, _) => {
            val (x2, y2) = toTileCoords(point)
            
            // only redo action if dragged to a different square
            if( (x2, y2) != (xLastDrag, yLastDrag) ) {
              repaintRegions(
                updateCursorSq(tool.selectionSqOnDrag, x2, y2),
                MapViewTools.selected.onMouseDragged(
                  vs, tCodes, x1, y1, x2, y2))
              
              xLastDrag = x2
              yLastDrag = y2
            }
          }
          case MouseReleased(`canvasPanel`, point, _, _, _) => {
            val (x2, y2) = toTileCoords(point)
            vs.commitNextData()
            
            repaintRegions(updateCursorSq(true, x2, y2))
            
            reactions -= temporaryReactions
          }
        }
        
        reactions += temporaryReactions
      }
    }
  }
}

object MapView {
  lazy val startLocTile = ImageIO.read(
    getClass.getClassLoader.getResourceAsStream("player_play.png"))
}
