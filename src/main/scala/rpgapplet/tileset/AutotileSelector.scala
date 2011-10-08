package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

class AutotileSelector(proj: Project, tilesetSidebar: TilesetSidebar) 
extends BoxPanel(Orientation.Vertical) {
  import Tileset.tilesize
  
  val autotiles : Vector[Autotile] = 
    proj.autotiles.map(Autotile.readFromDisk(proj, _))
  
  // draw every autotile onto collageImage in one huge row.
  // ImageSelector will group them into 8s
  val collageImage = new BufferedImage(autotiles.length*tilesize, 
                                       tilesize,
                                       BufferedImage.TYPE_4BYTE_ABGR)
  
  {
    val g = collageImage.createGraphics()
    
    autotiles.zipWithIndex map {
      case (autotile, i) => 
        g.drawImage(autotile.isolatedImg, i*tilesize, 0, null)
    }
  }
  
  // x coordiate corresponds to tileset number,
  // other two bytes we leave blank. 
  contents += new ImageTileSelector(collageImage, tXYArray => 
    tilesetSidebar.selectedTileCodes = tXYArray.map(_.map({
      case (xTile, yTile) => 
        (RpgMap.autotileByte, xTile, 0.asInstanceOf[Byte])
    }))
  )
}
