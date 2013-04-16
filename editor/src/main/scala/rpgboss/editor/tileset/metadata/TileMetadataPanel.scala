package rpgboss.editor.tileset.metadata

import scala.swing._
import rpgboss.editor.tileset._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.editor.lib._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster
import javax.imageio.ImageIO
import java.awt.geom.Line2D
import java.awt.AlphaComposite
import java.awt.Font
import java.awt.Color
import java.awt.event.MouseEvent

object MetadataMode extends RpgEnum {
  val PassabilityHeight = Value("Passability/Height")
  val DirectionalPassability = Value("Directional Passability")

  def default = PassabilityHeight
}

case class TileMetadata(blockedDirs: Byte, height: Byte) {
  import Constants.DirectionMasks._

  def passabilityHeightIncremented() = {
    if (!allBlocked(blockedDirs) && height == 0) {
      copy(blockedDirs = ALLCARDINAL.toByte)
    } else if (allBlocked(blockedDirs)) {
      copy(blockedDirs = NONE.toByte, height = 1)
    } else {
      copy(height = ((height + 1) % 6).toByte)
    }
  }

  def passabilityHeightDecremented() = {
    if (!allBlocked(blockedDirs) && height == 0) {
      copy(height = 5)
    } else if (allBlocked(blockedDirs)) {
      copy(blockedDirs = NONE.toByte)
    } else {
      copy(height = ((height - 1) % 6).toByte,
        blockedDirs = if (height == 1) ALLCARDINAL.toByte else NONE.toByte)
    }
  }
}

trait TileMetadataPanelOwner {
  /**
   * Returns a tuple of metadata i.e. Some((blockedDirs)) if tile exists
   * Return None if (xTile, yTile) is invalid
   */
  def getTileMeta(xTile: Int, yTile: Int): Option[TileMetadata]

  /**
   * User clicks this tile. Not required to do anything.
   */
  def updateTileMeta(xTile: Int, yTile: Int, newMetadata: TileMetadata): Unit

  var metadataMode = MetadataMode.default
}

class TileMetadataPanel(srcImg: BufferedImage, owner: TileMetadataPanelOwner)
  extends BoxPanel(Orientation.Horizontal) {
  import MetadataMode._

  def metadataMode = owner.metadataMode

  // Returns the new tile metadata as a result of the click
  def updatedMetadata(button: Int, xTile: Int,
                      yTile: Int): Option[TileMetadata] = {
    owner.getTileMeta(xTile, yTile) map { metadata =>
      import Constants.DirectionMasks._

      if (metadataMode == PassabilityHeight) {
        if (button == MouseEvent.BUTTON1) {
          metadata.passabilityHeightIncremented()
        } else {
          metadata.passabilityHeightDecremented()
        }
      } else {
        metadata
      }
    }
  }

  val tileClicker = new ImageTileSelector(
    srcImg = srcImg,
    selectTileF = { (button, tXYArray) =>
      val xTile = tXYArray.head.head._1
      val yTile = tXYArray.head.head._2
      updatedMetadata(button, xTile, yTile) map { newMetadata =>
        owner.updateTileMeta(xTile, yTile, newMetadata)
      }
    },
    allowMultiselect = false,
    drawSelectionSq = false) {

    val cl = getClass.getClassLoader

    def loadIcon(path: String) = ImageIO.read(cl.getResourceAsStream(path))

    val iconPass = loadIcon("tilesetMetadataIcons/all-pass.png")
    val iconStop = loadIcon("tilesetMetadataIcons/all-blocked.png")
    val iconArrow = loadIcon("tilesetMetadataIcons/all-blocked.png")
    val iconDirBlock = loadIcon("tilesetMetadataIcons/all-blocked.png")

    val iconHeights = for (i <- 0 to 5)
      yield loadIcon("tilesetMetadataIcons/height%d.png".format(i))

    override def canvasPanelPaintComponent(g: Graphics2D) = {
      super.canvasPanelPaintComponent(g)

      // All these values are in selector space
      val (minX, minY, maxX, maxY, minXTile, minYTile, maxXTile, maxYTile) =
        TileUtils.getTileBounds(
          g.getClipBounds(), tilesizeX, tilesizeY,
          xTilesVisible, imageSlices * yTilesInSlice)

      // Draw grid
      TileUtils.drawGrid(
        g, tilesizeX, tilesizeY, minXTile, minYTile, maxXTile, maxYTile)

      // Draw passabilty
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f))

      import Constants.DirectionMasks._

      def draw22Icon(icon: BufferedImage, xTile: Int, yTile: Int) = {
        val x1 = xTile * tilesizeX + (tilesizeX - 22) / 2
        val y1 = yTile * tilesizeY + (tilesizeY - 22) / 2
        g.drawImage(icon, x1, y1, null)
      }

      def draw32Icon(icon: BufferedImage, xTile: Int, yTile: Int) = {
        val x1 = xTile * tilesizeX
        val y1 = yTile * tilesizeY
        g.drawImage(icon, x1, y1, null)
      }

      /*
       * xTile and yTile are in selector space
       * xTileTS and yTileTS are in tileset space
       */
      for (
        yTile <- minYTile to maxYTile;
        xTile <- minXTile to maxXTile;
        (xTileTS, yTileTS) = toTilesetSpace(xTile, yTile);
        metadata <- owner.getTileMeta(xTileTS, yTileTS)
      ) {

        if (metadataMode == PassabilityHeight) {
          if (!allBlocked(metadata.blockedDirs)) {
            if (metadata.height == 0)
              draw22Icon(iconPass, xTile, yTile)
            else
              draw32Icon(iconHeights(metadata.height), xTile, yTile)
          } else if (allBlocked(metadata.blockedDirs))
            draw22Icon(iconStop, xTile, yTile)
          else
            None
        }
      }
    }
  }

  val outerThis = this

  contents += tileClicker
}
