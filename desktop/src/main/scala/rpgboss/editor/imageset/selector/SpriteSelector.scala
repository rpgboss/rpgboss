package rpgboss.editor.imageset.selector

import scala.swing._
import scala.swing.event._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.model.SpriteSpec
import rpgboss.editor.uibase._

/**
 * *
 * A selector that chooses an index of a sprite
 */
class SpriteSelector(
  spriteset: Spriteset,
  initialSpec: SpriteSpec,
  selectFunction: SpriteSpec => Any)
  extends BoxPanel(Orientation.Vertical) with DisposableComponent {

  import Spriteset._

  def initialX =
    (initialSpec.spriteIndex % nSpritesInSetX) * spriteXTiles + initialSpec.step
  def initialY =
    (initialSpec.spriteIndex / nSpritesInSetX) * spriteYTiles + initialSpec.dir

  // Set up image selector contents
  contents += new ImageTileSelector(
    spriteset.img,
    tilesizeX = spriteset.tileW,
    tilesizeY = spriteset.tileH,
    xTilesVisible = spriteXTiles * nSpritesInSetX,
    allowMultiselect = false,
    initialSelection = Some(((initialX, initialY), (initialX, initialY)))) {

    def selectTileF(button: Int, selectedTiles: Array[Array[(Int, Int)]]) = {
      // Since we are selecting only one, we only care about first element
      val (x1, y1) = selectedTiles.head.head

      // Compute the sprite index from the selected tile
      val spriteIdxX = x1 / spriteXTiles
      val spriteIdxY = y1 / spriteYTiles
      val spriteIdx = spriteIdxY * spriteset.xSprites + spriteIdxX

      val direction = y1 % spriteYTiles
      val step = x1 % spriteXTiles

      selectFunction(SpriteSpec(spriteset.name, spriteIdx, direction, step))
    }
  }
}