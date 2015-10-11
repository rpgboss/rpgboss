package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.FileHelper._

import org.json4s.native.Serialization

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

import java.io._
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import javax.imageio.ImageIO

/**
 * @param blockedDirs   Bit is on for each direction that is blocked.
 *                      Uses masks according to DirectionMasks
 *                      The the four "biggest" bits are undefined.
 * @param height        Used to indicate if it's an elevated tile. A zero value
 *                      is used for an ordinary tile.
 * @param vehicleDirs   An array of the same format is |blockedDirs|.
 *                      The only valid options are NONE and ALLCARDINAL.
 */
case class AutotileMetadata(blockedDirs: Byte = 0, height: Byte = 0,
                            vehicleDirs: Array[Byte] =
                              AutotileMetadata.defaultVehicleDirs) {
  def normalizedVehicleDirs = ArrayUtils.resized[Byte](
      vehicleDirs, Constants.NUM_VEHICLES,
      () => DirectionMasks.ALLCARDINAL.toByte)
}

object AutotileMetadata {
  def defaultVehicleDirs =
    Array.fill(Constants.NUM_VEHICLES)(DirectionMasks.ALLCARDINAL.toByte)
}

case class Autotile(proj: Project,
                    name: String,
                    metadata: AutotileMetadata)
  extends ImageResource[Autotile, AutotileMetadata]
  with LazyLogging {
  import Tileset.tilesize
  def meta = Autotile

  val terrainMode = img.getHeight() == 3 * tilesize
  val frames = if (terrainMode) {
    val frameWidth = 2 * tilesize
    img.getWidth() / frameWidth
  } else 1

  def isolatedImg() = {
    getTileImage(0xff, 0)
  }

  // autotileConfig must be a positive integer
  def getTileImage(autotileConfig: Int, frame: Byte) = {
    require(autotileConfig >= 0, "Autotile config integer must be positive.")
    require(frame >= 0, "Frame byte must be positive.")

    // logger.info("getTile (%d, %d) - %s".format(autotileConfig, frame, name))

    val tile =
      new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)

    val g = tile.createGraphics()
    val ht = tilesize / 2

    getHalfTiles(autotileConfig.toByte, frame) map {
      case ((srcXHt, srcYHt), (dstXHt, dstYHt)) =>
        g.drawImage(
          img,
          dstXHt * ht, dstYHt * ht,
          dstXHt * ht + ht, dstYHt * ht + ht,
          srcXHt * ht, srcYHt * ht,
          srcXHt * ht + ht, srcYHt * ht + ht,
          null)
    }

    tile
  }

  /**
   * Get the configuration of half-tiles needed to draw the autotile.
   *
   * See http://blog.rpgmakerweb.com/tutorials/anatomy-of-an-autotile/ for
   * how these autotiles are laid out.
   *
   *   "I mark minitiles with inside corners as 1, outside corners as 2,
   *   no borders as 3, north or south borders as 4, east or west borders as 5."
   *
   * Terrain autotiles:
   *
   *        A1 B1
   *   X    C1 D1
   *
   * A2 B4  A4 B2
   * C5 D3  C3 D5
   *
   * A5 B3  A3 B5
   * C2 D4  C4 D2
   *
   * Building autotiles:
   *
   * A2 B4  A4 B2
   * C5 D3  C3 D5
   *
   * A5 B3  A3 B5
   * C2 D4  C4 D2
   *
   */
  def getHalfTiles(borderConfig: Byte, frame: Int) = {
    require(frame >= 0, "Frame byte must be positive.")

    val frameI = frame % frames

    import Autotile._
    import DirectionMasks._
    import Autotile.SubtileOffsets._

    val AHt = getCornerHt(borderConfig, WEST, NORTH, NW, As)
    val BHt = getCornerHt(borderConfig, EAST, NORTH, NE, Bs)
    val CHt = getCornerHt(borderConfig, WEST, SOUTH, SW, Cs)
    val DHt = getCornerHt(borderConfig, EAST, SOUTH, SE, Ds)

    val dstPositions = Array(
      (0, 0), (1, 0), (0, 1), (1, 1))

    // Adjust offset for both Frame position dx = 4*frameI
    // and for terrain mode terrainMode ? 2 : 1
    val srcPositions = Array(AHt, BHt, CHt, DHt) map {
      case (xHt, yHt) => (xHt + 4 * frameI, yHt + (if (terrainMode) 2 else 0))
    }

    srcPositions zip dstPositions
  }
}

object Autotile extends MetaResource[Autotile, AutotileMetadata] {
  def rcType = "autotile"
  def keyExts = Array("png")

  def defaultInstance(proj: Project, name: String) =
    Autotile(proj, name, AutotileMetadata())

  /**
   * Get correct subtile associated with the borders
   */
  def getCornerHt(
    borderConfig: Byte,
    EWDir: Int,
    NSDir: Int,
    cornerDir: Int,
    subtile: Array[(Int, Int)]) = {

    val EWDiff = (borderConfig & EWDir) > 0
    val NSDiff = (borderConfig & NSDir) > 0
    val allSame = (borderConfig & (cornerDir | EWDir | NSDir)) == 0

    // If corner, eastwest, and northsouth are all the same.
    if (allSame) {
      subtile(SubtileOffsets.NoBorders)
    } else if (EWDiff && NSDiff) {
      subtile(SubtileOffsets.ExteriorCorner)
    } else { // corner is different
      if (EWDiff) {
        subtile(SubtileOffsets.EastWestBord)
      } else if (NSDiff) {
        subtile(SubtileOffsets.NorthSouthBord)
      } else {
        subtile(SubtileOffsets.InteriorCorner)
      }
    }
  }

  // In units of half-tiles. Origin is top-left corner of A2
  object SubtileOffsets {
    val A2 = (0, 0)
    val A4 = (2, 0)
    val A5 = (0, 2)
    val A3 = (2, 2)
    val A1 = (2, -2)

    val B4 = (1, 0)
    val B2 = (3, 0)
    val B3 = (1, 2)
    val B5 = (3, 2)
    val B1 = (3, -2)

    val C5 = (0, 1)
    val C3 = (2, 1)
    val C2 = (0, 3)
    val C4 = (2, 3)
    val C1 = (2, -1)

    val D3 = (1, 1)
    val D5 = (3, 1)
    val D4 = (1, 3)
    val D2 = (3, 3)
    val D1 = (3, -1)

    val As = Array(A1, A2, A3, A4, A5)
    val Bs = Array(B1, B2, B3, B4, B5)
    val Cs = Array(C1, C2, C3, C4, C5)
    val Ds = Array(D1, D2, D3, D4, D5)

    val InteriorCorner = 0 // A1
    val ExteriorCorner = 1 // A2
    val NoBorders = 2 // A3
    val NorthSouthBord = 3 // A4
    val EastWestBord = 4 // A5
  }
}
