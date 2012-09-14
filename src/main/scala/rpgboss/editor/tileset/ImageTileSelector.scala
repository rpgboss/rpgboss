package rpgboss.editor.tileset

import scala.math._
import scala.swing._
import scala.swing.event._

import rpgboss.lib.Utils._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._

import java.awt.image.BufferedImage
import java.awt.{Point, Color}

class ImageTileSelector(srcImg: FastImage,
                        selectTileF: Array[Array[(Byte, Byte)]] => Unit,
                        val tilesizeX : Int = 32,
                        val tilesizeY : Int = 32,
                        val xTilesVisible : Int = 8)
extends ScrollPane
{ 
  horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  
  // restrict to 256 by 256 tiles
  val img = srcImg.subimage(0, 0, 
                            min(255*tilesizeX, srcImg.width),
                            min(255*tilesizeY, srcImg.height))
  
  val imageSlices = ceilIntDiv(img.getWidth / tilesizeX, xTilesVisible)
  val yTiles = img.getHeight / tilesizeY
  
  peer.getViewport.setMinimumSize(
    new Dimension(xTilesVisible*tilesizeX, 4*tilesizeY))
  
  var xRngInSelectorSpace = 0 to 0
  var yRngInSelectorSpace = 0 to 0
  
  val canvasPanel = new Panel() {    
    preferredSize = new Dimension(xTilesVisible*tilesizeX, 
                                  imageSlices*img.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      
      for(i <- 0 until imageSlices) {
        g.drawImage(img, 
                    0, i*img.getHeight,
                    xTilesVisible*tilesizeX, (i+1)*img.getHeight,
                    i*xTilesVisible*tilesizeX, 0,
                    (i+1)*xTilesVisible*tilesizeX, img.getHeight,
                    null)
      }
      
      TileRect(xRngInSelectorSpace.head, yRngInSelectorSpace.head,
               xRngInSelectorSpace.length, yRngInSelectorSpace.length)
        .optionallyDrawSelRect(g, tilesizeX, tilesizeY)
    }
  }
  
  contents = canvasPanel
  
  listenTo(canvasPanel.mouse.clicks)
  listenTo(canvasPanel.mouse.moves)
  
  def toSelTiles(p: Point) = 
    (p.getX.toInt/tilesizeX, p.getY.toInt/tilesizeY)
  
  def toTilesetSpace(selTileX: Int, selTileY: Int) = {
    val tileX = selTileX + selTileY/yTiles*xTilesVisible
    val tileY = selTileY % yTiles
    (tileX.asInstanceOf[Byte], tileY.asInstanceOf[Byte])
  }
  
  def triggerSelectTileF() = {
    val selectedTiles = yRngInSelectorSpace.map(yTile => 
      xRngInSelectorSpace.map(xTile => toTilesetSpace(xTile, yTile)).toArray)
      .toArray
    selectTileF(selectedTiles)
  }
  
  // If xTile and yTile are within the bounds of the image in selector space
  def inBounds(xTile: Int, yTile: Int) =
    xTile < xTilesVisible && yTile < imageSlices*yTiles
  
  reactions += {
    
    case MousePressed(`canvasPanel`, point, _, _, _) => {
      val (x1, y1) = toSelTiles(point)
      if(inBounds(x1, y1)) {
        xRngInSelectorSpace = x1 to x1
        yRngInSelectorSpace = y1 to y1
        canvasPanel.repaint()
        
        lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
          case MouseDragged(`canvasPanel`, point, _) => {
            val (x2, y2) = toSelTiles(point)
            if(inBounds(x2, y2)) {
          
              xRngInSelectorSpace = min(x1, x2) to max(x1, x2)
              yRngInSelectorSpace = min(y1, y2) to max(y1, y2)
              canvasPanel.repaint()
            }
          }
          case MouseReleased(`canvasPanel`, point, _, _, _) => {
            triggerSelectTileF()
            reactions -= temporaryReactions
          }
        }
        
        reactions += temporaryReactions
      }
    }
  }
  
}
