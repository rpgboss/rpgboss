package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

class AutotileSelector(p: Project) 
extends BoxPanel(Orientation.Vertical) {
  import Tileset.tilesize
  
  val autotiles : Vector[Autotile] = 
    p.autotiles.map(Autotile.readFromDisk(p, _))
  
  // draw every autotile onto collageImage in one huge row.
  // ImageSelector will group them into 8s
  val collageImage = new BufferedImage(autotiles.length*tilesize, 
                                       tilesize,
                                       BufferedImage.TYPE_4BYTE_ABGR)
  
  {
    val g = collageImage.createGraphics()
    
    autotiles.zipWithIndex map {
      case (autotile, i) => 
        g.drawImage(autotile.representativeImg, i*tilesize, 0, null)
    }
  }
  
  contents += new ImageTileSelector(collageImage, t => Unit)
}
