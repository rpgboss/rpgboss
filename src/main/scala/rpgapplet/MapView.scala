package rpgboss.rpgapplet

import rpgboss.lib.Utils._
import rpgboss.cache._
import rpgboss.model._
import rpgboss.message._

import rpgboss.rpgapplet.tileset._
import rpgboss.rpgapplet.lib._

import scala.math._
import scala.swing._
import scala.swing.event._

//import java.awt._

case class MapInfo(map: RpgMap, mapData: RpgMapData, tilecache: TileCache)

class MapView(sm: StateMaster, tilesetSidebar: TilesetSidebar)
extends BoxPanel(Orientation.Vertical)
{
  var curMapInfo : Option[MapInfo] = None
  var curTilesize = Tileset.tilesize
    
  val pencilBtn    = new RadioButton("Pencil")
  val rectangleBtn = new RadioButton("Rectangle")
  val elipseBtn    = new RadioButton("Elipse")
  val selectBtn    = new RadioButton("Select")
  
  def scaleButton(title: String, invScale: Int) = new RadioButton() { 
    action = Action(title) {
      curTilesize = Tileset.tilesize / invScale
      canvasPanel.revalidate()
      canvasPanel.repaint()
    }
  } 
  
  val s11Btn = scaleButton("1/1", 1)
  val s12Btn = scaleButton("1/2", 2)
  val s14Btn = scaleButton("1/4", 4)
  
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    val tools = List(pencilBtn, rectangleBtn, elipseBtn, selectBtn)
    val toolsGrp = new ButtonGroup(tools : _*)
    toolsGrp.select(pencilBtn)
    
    val scales = List(s11Btn, s12Btn, s14Btn)
    val scalesGrp = new ButtonGroup(scales : _*)
    scalesGrp.select(s11Btn)
    
    contents ++= tools
    contents ++= scales
    
    contents += Swing.HGlue
  }
  
  val canvasPanel = new Panel() {
    var cursorSquare : Option[(Int, Int, Int, Int)] = None
    
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      
      curMapInfo.map(mapInfo => {
        def byteIdx(xTile: Int, yTile: Int) = 
          (xTile + (yTile*mapInfo.map.xSize))*RpgMap.bytesPerTile
        
        val bounds = g.getClipBounds
        val tilesize = curTilesize
        
        val minXTile = (bounds.getMinX/tilesize).toInt
        val minYTile = (bounds.getMinY/tilesize).toInt
        
        val maxXTile = 
          min(mapInfo.map.xSize-1, (bounds.getMaxX/tilesize).toInt)
        val maxYTile = 
          min(mapInfo.map.ySize-1, (bounds.getMaxY/tilesize).toInt)
        
        g.clip(new Rectangle(minXTile*tilesize, minYTile*tilesize,
                             (maxXTile-minXTile+1)*tilesize,
                             (maxYTile-minYTile+1)*tilesize))
        
        println("Paint Tiles: x: [%d,%d], y: [%d,%d]".format(
          minXTile, maxXTile, minYTile, maxYTile))
        
        // draw tiles
        mapInfo.mapData.drawOrder.map(curLayer => {
          for(xTile <- minXTile to maxXTile; yTile <- minYTile to maxYTile) {
            val bi = byteIdx(xTile, yTile)
            if(curLayer(bi) != -1) {
              val tileImg = mapInfo.tilecache.getTileImage(curLayer, bi, 0)
              
              g.drawImage(tileImg, 
                          xTile*tilesize, yTile*tilesize,
                          (xTile+1)*tilesize, (yTile+1)*tilesize,
                          0, 0, Tileset.tilesize, Tileset.tilesize,
                          null)
            }
          }   
        })
        
        // draw selection square
        cursorSquare map {
          case(xTile, yTile, widthTiles, heightTiles) =>
            GraphicsUtils.drawSelRect(g, xTile*curTilesize, yTile*curTilesize,
                                      widthTiles*curTilesize,
                                      heightTiles*curTilesize)
        }
        
        println("Canvaspanel size %s".format(size))
      })
    }
  }
  
  def selectMap(map: RpgMap) = {
    curMapInfo = map.readMapData(sm.proj).map(mapData => {
      val tilecache = new TileCache(sm.proj, map)
      Some(MapInfo(map, mapData, tilecache))
    }) getOrElse {
      Dialog.showMessage(this, "Map data file missing. Delete map.", 
                         "Error", 
                         Dialog.Message.Error)
      None
    }
    
    val mapDims = new Dimension(map.xSize*curTilesize, map.ySize*curTilesize) 
    canvasPanel.preferredSize = mapDims      
    
    canvasPanel.revalidate()
    canvasPanel.repaint()
  }
  
  contents += toolbar
  contents += new ScrollPane(canvasPanel) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Always
    verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  }
  
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  def toTileCoords(p: Point) = 
    (p.getX.toInt/curTilesize, p.getY.toInt/curTilesize) 
  
  reactions += {
    case MouseMoved(`canvasPanel`, p, _) => {
      val (tileX, tileY) = toTileCoords(p)
      
      val tCodes = tilesetSidebar.selectedTileCodes 
      assert(tCodes.length > 0 && tCodes(0).length > 0, "Selected tiles empty")
      
      val newCursorSq = Some((tileX, tileY, tCodes(0).length, tCodes.length))
      
      if(canvasPanel.cursorSquare != newCursorSq) {
        canvasPanel.cursorSquare = newCursorSq
        canvasPanel.repaint()
      }
    }
    case MouseExited(`canvasPanel`, _, _) =>
      canvasPanel.cursorSquare = None
      canvasPanel.repaint()
  }
}
