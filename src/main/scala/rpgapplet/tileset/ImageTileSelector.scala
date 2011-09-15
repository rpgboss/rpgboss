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
  
  val yTiles = srcImage.getHeight / tilesizeY
  
  val canvasPanel = new Panel() {
    minimumSize   = new Dimension(xTilesVisible*tilesizeX, 2*tilesizeY)
    preferredSize = new Dimension(xTilesVisible*tilesizeX, 
                                  srcImage.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      
      g.drawImage(srcImage, 0, 0,
                  xTilesVisible*tilesizeX, srcImage.getHeight, null)
      
      // draw selection square
      selection map {
        case (tileX, tileY) =>
          g.draw3DRect(tileX*tilesizeX, tileY*tilesizeY,
                       tilesizeX, tilesizeY, true)
      }
    }
  }
  
  contents = canvasPanel
  
  listenTo(canvasPanel)
  
  reactions += {
    case MouseClicked(`canvasPanel`, point, _, _, _) => {
      val tileX = point.getX.toInt/tilesizeX
      val tileY = point.getY.toInt/tilesizeY 
      
      selection = Some((tileX, tileY))
      selection.foreach(selectTileF)
    }
  }
  
}

object ImageTileSelector {
}
