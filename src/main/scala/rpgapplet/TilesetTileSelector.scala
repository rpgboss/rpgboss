package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

import java.awt.Graphics2D

/*
  tileset: must be guaranteed to be the correct dimensions as
           specified by the tileset metadata (tilesize*xTiles, tilesize*yTiles)
*/
class TilesetTileSelector(tileset: BufferedImage, 
                          selectTileF: ((Int, Int)) => Unit,
                          initialSelection: Option[(Int, Int)] = Some((0,0)))
extends ScrollPane
{
  import Tileset._
  import TilesetTileSelector._
  
  minimumSize = new Dimension(640, 480)
  
  def imageSlices = ceilIntDiv(tileset.getWidth / tilesize, xTilesVisible)
  
  // x coord has max of xTilesVisible, then it wraps around
  var selectedTileInSelectorSpace : Option[(Int, Int)] = initialSelection
  
  def selectedTileInTilesetSpace = selectedTileInSelectorSpace.map { 
    case (selTileX, selTileY) => {
      val yTiles = tileset.getHeight / tilesize
        
      val tileX = selTileX + selTileY/yTiles*xTilesVisible
      val tileY = selTileY % yTiles
        
      (tileX, tileY)
    }
  }
  
  val canvasPanel = new Panel() {
    preferredSize = new Dimension(xTilesVisible*tilesize, 
                                  imageSlices*tileset.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      
      for(i <- 0 until imageSlices) {
        g.drawImage(tileset, 
                    0, i*tileset.getHeight,
                    xTilesVisible*tilesize, (i+1)*tileset.getHeight,
                    i*xTilesVisible*tilesize, 0,
                    (i+1)*xTilesVisible*tilesize, tileset.getHeight,
                    null)
      }
      
      // draw selection square
      selectedTileInSelectorSpace map {
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
      selectedTileInSelectorSpace = 
        Some((point.getX.toInt/tilesize, point.getY.toInt/tilesize))
      selectedTileInTilesetSpace.foreach(selectTileF)
    }
  }
  
}

object TilesetTileSelector {
  val xTilesVisible = 8
  
  // does ceil integer division, Number Conversion, Roland Backhouse, 2001
  // http://stackoverflow.com/questions/17944/
  def ceilIntDiv(n: Int, m: Int) = (n-1)/m + 1
}
