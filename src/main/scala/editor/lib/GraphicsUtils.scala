package rpgboss.editor.lib

import rpgboss.model._
import java.awt._

object GraphicsUtils {
  def drawSelRect(g: Graphics, x1: Int, y1: Int, w: Int, h: Int) = {
    g.setColor(Color.BLACK)
    // need additional -1 because 
    // "The left and right edges of the rectangle are at x and x + width"
    g.drawRect(x1, y1, w-1, h-1)
    g.drawRect(x1+3, y1+3, w-6-1, h-6-1)
    g.setColor(Color.WHITE)
    g.drawRect(x1+1, y1+1, w-2-1, h-2-1)
    g.drawRect(x1+2, y1+2, w-4-1, h-4-1)
  }
  
  // x2, y2, must be greater than x1, y1
  def tileRect(xTile: Int, yTile: Int, 
               widthTiles: Int = 1, heightTiles: Int = 1) = 
  {
    val x = xTile*Tileset.tilesize
    val w = widthTiles*Tileset.tilesize
    val y = yTile*Tileset.tilesize
    val h = heightTiles*Tileset.tilesize
    new Rectangle(x, y, w, h)
  }
  
  def NilRect() = new Rectangle(0, 0, -1, -1)
}
