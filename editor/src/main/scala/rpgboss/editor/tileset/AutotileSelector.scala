package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster

class AutotileSelector( 
    proj: Project,
    map: RpgMap,
    tileSelector: TabbedTileSelector) 
  extends BoxPanel(Orientation.Vertical) {
  
  val autotiles = map.metadata.autotiles.map(Autotile.readFromDisk(proj, _))
  val collageImage = TileUtils.getAutotileCollageImg(autotiles)
  
  // x coordiate corresponds to tileset number,
  // other two bytes we leave blank. 
  contents += new ImageTileSelector(collageImage, tXYArray => 
    tileSelector.selectedTileCodes = tXYArray.map(_.map({
      case (xTile, yTile) => 
        Array(RpgMap.autotileByte, xTile, 0.asInstanceOf[Byte])
    }))
  )
}
