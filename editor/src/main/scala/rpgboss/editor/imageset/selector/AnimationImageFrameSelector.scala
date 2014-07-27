package rpgboss.editor.imageset.selector

import scala.swing._
import scala.swing.event._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.model._
import rpgboss.editor.resourceselector.ResourceRightPane

/**
 * Chooses a frame of an AnimationImage
 */
class AnimationImageFrameSelector(
  animationImage: AnimationImage,
  initialSpec: AnimationKeyframe,
  selectFunction: AnimationKeyframe => Any)
  extends BoxPanel(Orientation.Vertical) with ResourceRightPane {

  import Spriteset._

  val xTiles = animationImage.xTiles
  val initialX = initialSpec.frameIndex % xTiles
  val initialY = initialSpec.frameIndex / xTiles

  // Set up image selector contents
  contents += new ImageTileSelector(
    animationImage.img,
    tilesizeX = animationImage.tileW,
    tilesizeY = animationImage.tileH,
    xTilesVisible = 400 / animationImage.tileW,
    allowMultiselect = false,
    initialSelection = Some((initialX, initialY))) {

    def selectTileF(button: Int, selectedTiles: Array[Array[(Int, Int)]]) = {
      val (x1, y1) = selectedTiles.head.head

      selectFunction(initialSpec.copy(frameIndex = y1 * xTiles + x1))
    }
  }
}