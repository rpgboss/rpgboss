package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

import java.awt.Graphics2D

/*
  tilesetImg: must be guaranteed to be the correct dimensions as
    specified by the tileset metadata (tilesize*xTiles, tilesize*yTiles)
*/
class TileSelector(tilesetImg: BufferedImage, 
                   selectTileF: ((Int, Int)) => Unit,
                   private var selection: (Int, Int) = (0,0))
extends ScrollPane
{
  import Tileset._
  import TileSelector._
  
  minimumSize = new Dimension(640, 480)
  
  def imageSlices = ceilIntDiv(tileset.getWidth / tilesize, xTilesVisible)
  
  val canvasPanel = new Panel() {
    preferredSize = new Dimension(xTilesVisible*tilesize, 
                                  imageSlices*tilesetImg.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      
      for(i <- 0 until imageSlices) {
        g.drawImage(tilesetImg, 
                    0, i*tilesetImg.getHeight,
                    xTilesVisible*tilesize, (i+1)*tilesetImg.getHeight,
                    i*xTilesVisible*tilesize, 0,
                    (i+1)*xTilesVisible*tilesize, tilesetImg.getHeight,
                    null)
      }
      
      // draw selection square
      selectionInSelectorSpace map {
        case (selTileX, selTileY) => g.draw3DRect(selTileX*tilesize,
                                                  selTileY*tilesize,
                                                  tilesize, tilesize, true)
      }
    }
  }
  
  contents = canvasPanel
  
  listenTo(canvasPanel)
  
  reactions += {
    case MouseClicked(`canvasPanel`, point, _, _, _) => {
      var selTileX = 
      selectionInSelectorSpace = 
        Some((point.getX.toInt/tilesize, point.getY.toInt/tilesize))
      selectionInTilesetSpace.foreach(selectTileF)
      
      val yTiles = tilesetImg.getHeight / tilesize
        
      val tileX = selTileX + selTileY/yTiles*xTilesVisible
      val tileY = selTileY % yTiles
        
      (tileX, tileY)
    }
  }
  
}

object TileSelector {
  val xTilesVisible = 8
  
  // does ceil integer division, Number Conversion, Roland Backhouse, 2001
  // http://stackoverflow.com/questions/17944/
  def ceilIntDiv(n: Int, m: Int) = (n-1)/m + 1
}
