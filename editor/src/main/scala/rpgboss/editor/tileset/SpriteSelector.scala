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
    initialSpec: SpriteSpec,
    selectFunction: SpriteSpec => Any)
  extends BoxPanel(Orientation.Vertical) {
  
  import Spriteset._
  def selectTileF(button: Int, twoDAry: Array[Array[(Int, Int)]]) = {
    // Since we are selecting only one, we only care about first element
    val (x1, y1) = twoDAry.head.head
    
    // Compute the sprite index from the selected tile 
    val spriteIdxX = x1/spriteXTiles
    val spriteIdxY = y1/spriteYTiles
    val spriteIdx  = spriteIdxY*spriteset.xSprites+spriteIdxX
    
    val direction = y1 % spriteYTiles
    val step      = x1 % spriteXTiles
    
    selectFunction(SpriteSpec(spriteset.name, spriteIdx, direction, step))
  }
  
  def initialX = 
    (initialSpec.spriteIndex % nSpritesInSetX)*spriteXTiles + initialSpec.step
  def initialY = 
    (initialSpec.spriteIndex / nSpritesInSetX)*spriteYTiles + initialSpec.dir
  
  // Set up image selector contents
  contents += new ImageTileSelector(
      spriteset.img,
      selectTileF = selectTileF _,
      tilesizeX = spriteset.tileW,
      tilesizeY = spriteset.tileH,
      xTilesVisible = spriteXTiles*nSpritesInSetX,
      allowMultiselect = false,
      initialSingleSelTilesetSpace = Some((initialX, initialY))
  )
}