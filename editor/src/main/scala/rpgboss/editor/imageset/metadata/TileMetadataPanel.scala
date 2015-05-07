package rpgboss.editor.imageset.metadata

import java.awt.AlphaComposite
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage

import scala.swing._

import rpgboss.editor.StateMaster
import rpgboss.editor.imageset._
import rpgboss.editor.imageset.selector._
import rpgboss.editor.Internationalized._
import rpgboss.editor.misc.TileUtils
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.lib.ArrayUtils
import rpgboss.model._

object MetadataMode extends RpgEnum {
  val PassabilityHeight = Value("Passability_Height")
  val DirectionalPassability = Value("Directional_Passability")
  val VehiclePassability0 = Value("Vehicle_1_Passability")
  val VehiclePassability1 = Value("Vehicle_2_Passability")
  val VehiclePassability2 = Value("Vehicle_3_Passability")
  val VehiclePassability3 = Value("Vehicle_4_Passability")

  def getVehicleIdForMode(value: Value) = {
    value match {
      case VehiclePassability0 => 0
      case VehiclePassability1 => 1
      case VehiclePassability2 => 2
      case VehiclePassability3 => 3
      case _ => -1
    }
  }

  def default = PassabilityHeight
}

case class TileMetadata(blockedDirs: Byte, height: Byte,
                        vehicleDirs: Array[Byte]) {
  import DirectionMasks._

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

  def directionalPassabilityToggled(xInTile: Int, yInTile: Int,
                                    tilesizeX: Int, tilesizeY: Int) = {
    val xInTileCenterOrigin = xInTile - tilesizeX / 2;
    val yInTileCenterOrigin = yInTile - tilesizeY / 2;

    val direction =
      if (yInTileCenterOrigin > xInTileCenterOrigin) {
        if (yInTileCenterOrigin > -xInTileCenterOrigin) NORTH else WEST
      } else {
        if (yInTileCenterOrigin > -xInTileCenterOrigin) EAST else SOUTH
      }

    copy(blockedDirs = (blockedDirs ^ direction).toByte) // Flip direction bit.
  }

  def vehiclePassabilityToggled(vehicleId: Int) = {
    val newVehicleDirs =
      ArrayUtils.resized[Byte](vehicleDirs, Constants.NUM_VEHICLES,
          () => DirectionMasks.NONE.toByte)

    val oldValue = newVehicleDirs(vehicleId)
    val newValue = if (oldValue == DirectionMasks.NONE)
      DirectionMasks.ALLCARDINAL.toByte
    else
      DirectionMasks.NONE.toByte
    newVehicleDirs.update(vehicleId, newValue)

    copy(vehicleDirs = newVehicleDirs)
  }
}

class TileMetadataPanel(srcImg: BufferedImage, owner: TilesetsMetadataPanel,
                        canUpdateVehicleDirs: Boolean)
  extends BoxPanel(Orientation.Horizontal) {
  import MetadataMode._

  def metadataMode = owner.metadataMode

  val tileClicker: ImageTileSelector = new ImageTileSelector(
    srcImg = srcImg,
    allowMultiselect = false,
    drawSelectionSq = false) {

    // Returns the new tile metadata as a result of the click
    def updatedMetadata(button: Int, xTile: Int, yTile: Int,
                        xInTile: Int, yInTile: Int): TileMetadata = {
      val metadata = owner.getTileMeta(xTile, yTile).get
      import DirectionMasks._

      if (metadataMode == PassabilityHeight) {
        if (button == MouseEvent.BUTTON1) {
          metadata.passabilityHeightIncremented()
        } else {
          metadata.passabilityHeightDecremented()
        }
      } else if (metadataMode == DirectionalPassability) {
        metadata.directionalPassabilityToggled(
          xInTile, yInTile, tilesizeX, tilesizeY)
      } else {
        val vehicleId = MetadataMode.getVehicleIdForMode(metadataMode)
        if (vehicleId >= 0) {
          if (canUpdateVehicleDirs) {
            metadata.vehiclePassabilityToggled(vehicleId)
          } else {
            Dialog.showMessage(
                this,
                getMessage("Cannot_update_vehicle_passibility"))
            metadata
          }
        } else {
          metadata
        }
      }
    }

    def selectTileF(button: Int, selectedTiles: Array[Array[(Int, Int)]]) = {}

    override def mousePressed(button: Int, xTile: Int, yTile: Int, xInTile: Int,
                              yInTile: Int) = {
      if (owner.inBounds(xTile, yTile)) {
        val newMetadata =
          updatedMetadata(button, xTile, yTile, xInTile, yInTile)
        owner.updateTileMeta(xTile, yTile, newMetadata)
      }
    }

    def loadIcon(path: String) = rpgboss.lib.Utils.readClasspathImage(path)

    val iconPass = loadIcon("tilesetMetadataIcons/all-pass.png")
    val iconStop = loadIcon("tilesetMetadataIcons/all-blocked.png")
    val iconNorthPass = loadIcon("tilesetMetadataIcons/north-pass.png")
    val iconNorthStop = loadIcon("tilesetMetadataIcons/north-stop.png")
    val iconSouthPass = loadIcon("tilesetMetadataIcons/south-pass.png")
    val iconSouthStop = loadIcon("tilesetMetadataIcons/south-stop.png")
    val iconEastPass = loadIcon("tilesetMetadataIcons/east-pass.png")
    val iconEastStop = loadIcon("tilesetMetadataIcons/east-stop.png")
    val iconWestPass = loadIcon("tilesetMetadataIcons/west-pass.png")
    val iconWestStop = loadIcon("tilesetMetadataIcons/west-stop.png")

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

      import DirectionMasks._

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

      def drawDirectionalPassability(blockedDirs: Byte, xTile: Int,
                                     yTile: Int) = {
        if ((blockedDirs & NORTH) == NORTH)
          draw32Icon(iconNorthStop, xTile, yTile)
        else
          draw32Icon(iconNorthPass, xTile, yTile)

        if ((blockedDirs & WEST) == WEST)
          draw32Icon(iconWestStop, xTile, yTile)
        else
          draw32Icon(iconWestPass, xTile, yTile)

        if ((blockedDirs & SOUTH) == SOUTH)
          draw32Icon(iconSouthStop, xTile, yTile)
        else
          draw32Icon(iconSouthPass, xTile, yTile)

        if ((blockedDirs & EAST) == EAST)
          draw32Icon(iconEastStop, xTile, yTile)
        else
          draw32Icon(iconEastPass, xTile, yTile)
      }

      /*
       * xTile and yTile are in selector space
       * xTileTS and yTileTS are in tileset space
       */
      for (
        yTile <- minYTile to maxYTile;
        xTile <- minXTile to maxXTile;
        (xTileTS, yTileTS) = toTilesetSpace(xTile, yTile);
        if (owner.inBounds(xTileTS, yTileTS));
        metadata <- owner.getTileMeta(xTileTS, yTileTS)
      ) {

        if (metadataMode == PassabilityHeight) {
          if (!allBlocked(metadata.blockedDirs)) {
            if (metadata.height == 0) {
              if ((metadata.blockedDirs & ALLCARDINAL) == 0)
                draw22Icon(iconPass, xTile, yTile)
              else
                drawDirectionalPassability(metadata.blockedDirs, xTile, yTile)
            } else
              draw32Icon(iconHeights(metadata.height), xTile, yTile)
          } else
            draw22Icon(iconStop, xTile, yTile)
        } else if (metadataMode == DirectionalPassability) {
          drawDirectionalPassability(metadata.blockedDirs, xTile, yTile)
        } else {
          val vehicleId = MetadataMode.getVehicleIdForMode(metadataMode)
          if (vehicleId >= 0) {
            if (allBlocked(metadata.vehicleDirs(vehicleId)))
              draw22Icon(iconStop, xTile, yTile)
            else
              draw22Icon(iconPass, xTile, yTile)
          }
        }
      }
    }
  }

  val outerThis = this

  contents += tileClicker
}
