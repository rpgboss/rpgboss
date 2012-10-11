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
    val g = collageImage.getGraphics()
    
    for(spriteX <- 0 until spriteset.xSprites;
        spriteY <- 0 until spriteset.ySprites) {
      val spriteIdx = spriteY*spriteset.xSprites+spriteX
      
      // Tile positions of representative images within spriteset image
      val tileI = (spriteX*3+1)
      val tileJ = (spriteY*4)
      
      spriteset.drawTileAt(tileI, tileJ, g, spriteIdx*spriteset.tileW, 0)
    }
  }
  
  // Set up image selector contents
  contents += new ImageTileSelector(
      collageImage,
      selectTileF = (twoDAry: Array[Array[(Byte, Byte)]]) => {
        // Since we are selecting only one, we only care about first element
        val (x1, y1) = twoDAry.head.head
        
        val spriteIdx = y1*spriteset.ySprites+x1
        selectFunction(spriteIdx)
      },
      tilesizeX = spriteset.tileW,
      tilesizeY = spriteset.tileH,
      xTilesVisible = 4,
      allowMultiselect = false
  )
}