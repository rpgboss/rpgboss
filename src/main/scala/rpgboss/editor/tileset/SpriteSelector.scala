package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.model.SpriteSpec

/***
 * A selector that chooses an index of a sprite
 */
class SpriteSelector(
    spriteset: Spriteset, 
    selectFunction: SpriteSpec => Any)
  extends BoxPanel(Orientation.Vertical) {
  
  def selectTileF(twoDAry: Array[Array[(Byte, Byte)]]) = {
    // Since we are selecting only one, we only care about first element
    val (x1, y1) = twoDAry.head.head
    
    // Compute the sprite index from the selected tile 
    val spriteIdxX = x1/spriteset.spriteXTiles
    val spriteIdxY = y1/spriteset.spriteYTiles
    val spriteIdx  = spriteIdxY*spriteset.xSprites+spriteIdxX
    
    val direction = y1 % spriteset.spriteYTiles
    val step      = x1 % spriteset.spriteXTiles
    
    selectFunction(SpriteSpec(spriteset.name, spriteIdx, direction, step))
  }
  
  // Set up image selector contents
  contents += new ImageTileSelector(
      spriteset.img,
      selectTileF = selectTileF _,
      tilesizeX = spriteset.tileW,
      tilesizeY = spriteset.tileH,
      xTilesVisible = 3*4,
      allowMultiselect = false
  )
}