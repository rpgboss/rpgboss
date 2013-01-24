package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster

class AutotileSelector( 
    sm: StateMaster,
    map: RpgMap,
    tileSelector: TabbedTileSelector) 
  extends BoxPanel(Orientation.Vertical) {
  
  val autotiles = 
    map.metadata.autotiles.map(sm.assetCache.getAutotile(_))
  val collageImage = TileUtils.getAutotileCollageImg(autotiles)
  
  val imgTileSelector = new ImageTileSelector(collageImage, tXYArray => 
    tileSelector.selectedTileCodes = tXYArray.map(_.map({
      case (xTile, yTile) => 
        Array(RpgMap.autotileByte, xTile.toByte, 0.toByte)
    }))
  )
  
  // x coordiate corresponds to tileset number,
  // other two bytes we leave blank. 
  contents += imgTileSelector 
  
  preferredSize = imgTileSelector.preferredSize
  
}
