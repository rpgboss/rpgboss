package rpgboss.editor.lib

import rpgboss.model._

import scala.math._
import java.awt._

object GraphicsUtils {
  // This function defined in units of pixels
  def drawSelRect(g: Graphics2D, rect: Rectangle) = {
    val x1 = rect.getMinX.toInt
    val y1 = rect.getMinY.toInt
    val h = rect.getHeight.toInt
    val w = rect.getWidth.toInt
    g.setStroke(new BasicStroke())
    g.setComposite(AlphaComposite.SrcOver)
    g.setColor(Color.BLACK)
    // need additional -1 because 
    // "The left and right edges of the rectangle are at x and x + width"
    g.drawRect(x1, y1, w-1, h-1)
    g.drawRect(x1+3, y1+3, w-6-1, h-6-1)
    g.setColor(Color.WHITE)
    g.drawRect(x1+1, y1+1, w-2-1, h-2-1)
    g.drawRect(x1+2, y1+2, w-4-1, h-4-1)
  }
  
  case class TileRect(x1: Int, y1: Int, 
                      wTiles: Int = 1, hTiles: Int = 1) 
  {
    def x2 = x1+wTiles-1
    def y2 = y1+hTiles-1
    def empty = wTiles < 1 || hTiles < 1
    
    def |(o: TileRect) =  if(o.empty) this else
      TileRect(min(x1, o.x1), min(y1, o.y1),
               max(x2, o.x2)-min(x1, o.x1)+1,
               max(y2, o.y2)-min(y1, o.y1)+1)
   
    def rect(xTilesize: Int, yTilesize: Int) =
      if(empty) new Rectangle(0, 0, -1, -1)
      else new Rectangle(x1*xTilesize, y1*yTilesize, 
                         wTiles*xTilesize, hTiles*yTilesize)
    
    def optionallyDrawSelRect(g: Graphics2D, xTilesize: Int, yTilesize: Int) = 
      if(!empty) drawSelRect(g, rect(xTilesize, yTilesize))
  }
  
  object TileRect {
    def apply() : TileRect = TileRect(0, 0, -1, -1)
  }
  
  case class IntVec(tup: Tuple2[Int, Int]) {
    def x = tup._1
    def y = tup._2
    def +(other: IntVec) = IntVec((x+other.x, y+other.y))
  }
  
  implicit def toIntVec(tup: Tuple2[Int, Int]) = IntVec(tup)
}
