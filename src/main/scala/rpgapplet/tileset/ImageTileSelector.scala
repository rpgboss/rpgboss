package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

import java.awt.Graphics2D

class ImageTileSelector(srcImage: BufferedImage,
                        selectTileF: ((Int, Int)) => Unit,
                        val tilesizeX : Int = 32,
                        val tilesizeY : Int = 32,
                        val xTilesVisible : Int = 8,
                        var selection: Option[(Int, Int)] = Some((0,0)))
extends ScrollPane
{
  import ImageTileSelector._
  
  val imageSlices = ceilIntDiv(srcImage.getWidth / tilesizeX, xTilesVisible)
  val yTiles = srcImage.getHeight / tilesizeY
  
  val canvasPanel = new Panel() {
    minimumSize   = new Dimension(xTilesVisible*tilesizeX, 2*tilesizeY)
    preferredSize = new Dimension(xTilesVisible*tilesizeX, 
                                  imageSlices*srcImage.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      
      for(i <- 0 until imageSlices) {
        g.drawImage(srcImage, 
                    0, i*srcImage.getHeight,
                    xTilesVisible*tilesizeX, (i+1)*srcImage.getHeight,
                    i*xTilesVisible*tilesizeX, 0,
                    (i+1)*xTilesVisible*tilesizeX, srcImage.getHeight,
                    null)
      }
      
      // draw selection square
      selection map {
        case (tileX, tileY) => {
          val selTileX = tileX % xTilesVisible
          val selTileY = ((tileX / xTilesVisible)*yTiles) + tileY
          
          g.draw3DRect(selTileX*tilesizeX,
                       selTileY*tilesizeY,
                       tilesizeX, tilesizeY, true)
        }
      }
    }
  }
  
  contents = canvasPanel
  
  listenTo(canvasPanel)
  
  reactions += {
    case MouseClicked(`canvasPanel`, point, _, _, _) => {
      val selTileX = point.getX.toInt/tilesizeX
      val selTileY = point.getY.toInt/tilesizeY 
        
      val tileX = selTileX + selTileY/yTiles*xTilesVisible
      val tileY = selTileY % yTiles
      
      selection = Some((tileX, tileY))
      selection.foreach(selectTileF)
    }
  }
  
}

object ImageTileSelector {
  // does ceil integer division, Number Conversion, Roland Backhouse, 2001
  // http://stackoverflow.com/questions/17944/
  def ceilIntDiv(n: Int, m: Int) = (n-1)/m + 1
}
