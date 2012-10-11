package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._
import rpgboss.model.resource._
import java.awt.image.BufferedImage

/***
 * A selector that chooses an index of a sprite
 */
class SpriteIndexSelector(spriteset: Spriteset, selectFunction: Int => Any)
  extends BoxPanel(Orientation.Vertical) {
  
  val collageImage = new BufferedImage(spriteset.nSprites*spriteset.tileW, 
                                       spriteset.tileH,
                                       BufferedImage.TYPE_4BYTE_ABGR)
  
  // Draw up bufferedImage
  {
    
  }

}