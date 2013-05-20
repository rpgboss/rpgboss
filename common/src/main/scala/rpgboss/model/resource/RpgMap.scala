package rpgboss.model.resource

import rpgboss.model._
import rpgboss.lib._
import rpgboss.lib.FileHelper._
import org.json4s.native.Serialization
import scala.collection.JavaConversions._
import java.io._
import java.util.Arrays
import org.json4s.DefaultFormats

case class RpgMapMetadata(var parent: String,
                          var title: String,
                          var xSize: Int,
                          var ySize: Int,
                          var tilesets: Array[String],
                          var autotiles: Array[String],
                          var changeMusicOnEnter: Boolean = false,
                          var music: Option[SoundSpec] = None,
                          var editorCenterX: Float = 0f,
                          var editorCenterY: Float = 0f) {
  def withinBounds(x: Float, y: Float) = {
    x < xSize && y < ySize && x >= 0 && y >= 0
  }
  def withinBounds(x: Int, y: Int) = {
    x < xSize && y < ySize && x >= 0 && y >= 0
  }
}

/**
 * @param   name  This is the "id" of the map, and should never be changed
 *                once assigned. Otherwise, we get into these messy issues:
 *                 * Need to rename data file and metadata file
 *                 * Need to update the "parent" field of all its children
 *                 * Need to update all the events.
 *
 *                Instead, we should use the "title" field of the metadata
 *                for all cases where we need to refer to the title.
 */
case class RpgMap(proj: Project, name: String, metadata: RpgMapMetadata)
  extends Resource[RpgMap, RpgMapMetadata] {
  def meta = RpgMap

  def saveMapData(d: RpgMapData) =
    d.writeToFile(proj, name)

  def readMapData(): Option[RpgMapData] =
    RpgMapData.readFromDisk(proj, name)

  def id = name.split("\\.").head
  def displayId = "%s [%s]".format(metadata.title, id)
  def displayName =
    if (metadata.title.isEmpty()) "[%s]".format(id) else metadata.title
}

/*
 * An explanation of the data format.
 * 
 * Each tile on the map is comprised of 3 bytes.
 * 
 * Byte 1 value:
 * -2 = autotile
 * -1 = empty tile
 * 0-127 = one of the 128 tilesets possible
 * 
 * Byte 2 value:
 * If autotile, then the autotile number from 0-255
 * If regular tile, then x tile index ranging from 0-255
 * If empty, ignored.
 * 
 * Byte 3 value:
 * If autotile, then this byte describes the border configuration.
 *    See Autotile.DirectionMasks for how this works specifically.
 * If regular tile, then the y tile index from 0-255
 * If empty, ignored
 */
object RpgMap extends MetaResource[RpgMap, RpgMapMetadata] {
  def rcType = "rpgmap"
  def mapExt = "rpgmap"
  def keyExts = Array(mapExt)

  val minXSize = 20
  val minYSize = 15

  val maxXSize = 500
  val maxYSize = 500

  val initXSize = 40
  val initYSize = 30

  val bytesPerTile = 3

  val autotileByte: Byte = -2
  val emptyTileByte: Byte = -1

  def autotileSeed = Array[Byte](autotileByte, 0, 0)
  def emptyTileSeed = Array[Byte](emptyTileByte, 0, 0)

  /**
   * Generates an array made the seed bytes, repeated
   */
  def makeRowArray(nTiles: Int, seed: Array[Byte]) = {
    Array.tabulate[Byte](nTiles * bytesPerTile)(i => seed(i % bytesPerTile))
  }

  def generateName(id: Int) =
    "Map%06d.%s".formatLocal(java.util.Locale.US, id, mapExt)

  def defaultInstance(proj: Project, name: String) = {
    val idxOfDot = name.indexOf(".")
    val title = if (idxOfDot > 0) name.substring(0, idxOfDot) else name
    val m = RpgMapMetadata(
      "", title,
      initXSize, initYSize,
      defaultTilesets,
      defaultAutotiles)
    apply(proj, name, m)
  }

  def emptyMapData(xSize: Int, ySize: Int) = {
    val autoLayer = {
      // Make a whole row of that autotile triples
      val row = makeRowArray(xSize, autotileSeed)
      // Make multiple rows
      Array.fill(ySize)(row.clone())
    }
    val emptyLayer = {
      val row = makeRowArray(xSize, emptyTileSeed)
      Array.fill(ySize)(row.clone())
    }

    RpgMapData(autoLayer, emptyLayer, emptyLayer, Array.empty)
  }

  def defaultMapData = emptyMapData(initXSize, initYSize)

  def defaultTilesets = Array(
    "Refmap-TileA5.png",
    "Refmap-TileB.png",
    "Refmap-TileC.png",
    "Refmap-TileD.png",
    "Refmap-TileE.png")

  def defaultAutotiles = Array(
    "Refmap-A1-0-0-A.png",
    "Refmap-A1-0-0-B.png",
    "Refmap-A1-0-1-A.png",
    "Refmap-A1-0-1-B.png",
    "Refmap-A1-1-0-A.png",
    "Refmap-A1-1-0-B.png",
    "Refmap-A1-1-1-A.png",
    "Refmap-A1-1-1-B.png",
    "Refmap-A1-2-0-A.png",
    "Refmap-A1-2-0-B.png",
    "Refmap-A1-2-1-A.png",
    "Refmap-A1-2-1-B.png",
    "Refmap-A1-3-0-A.png",
    "Refmap-A1-3-0-B.png",
    "Refmap-A1-3-1-A.png",
    "Refmap-A1-3-1-B.png",
    "Refmap-A2-0-0.png",
    "Refmap-A2-0-1.png",
    "Refmap-A2-0-2.png",
    "Refmap-A2-0-3.png",
    "Refmap-A2-0-4.png",
    "Refmap-A2-0-5.png",
    "Refmap-A2-0-6.png",
    "Refmap-A2-0-7.png",
    "Refmap-A2-1-0.png",
    "Refmap-A2-1-1.png",
    "Refmap-A2-1-2.png",
    "Refmap-A2-1-3.png",
    "Refmap-A2-1-4.png",
    "Refmap-A2-1-5.png",
    "Refmap-A2-1-6.png",
    "Refmap-A2-1-7.png",
    "Refmap-A2-2-0.png",
    "Refmap-A2-2-1.png",
    "Refmap-A2-2-2.png",
    "Refmap-A2-2-3.png",
    "Refmap-A2-2-4.png",
    "Refmap-A2-2-5.png",
    "Refmap-A2-2-6.png",
    "Refmap-A2-2-7.png",
    "Refmap-A2-3-0.png",
    "Refmap-A2-3-1.png",
    "Refmap-A2-3-2.png",
    "Refmap-A2-3-3.png",
    "Refmap-A2-3-4.png",
    "Refmap-A2-3-5.png",
    "Refmap-A2-3-6.png",
    "Refmap-A2-3-7.png",
    "Refmap-A3-0-0-A.png",
    "Refmap-A3-0-1-A.png",
    "Refmap-A3-0-2-A.png",
    "Refmap-A3-0-3-A.png",
    "Refmap-A3-0-4-A.png",
    "Refmap-A3-0-5-A.png",
    "Refmap-A3-0-6-A.png",
    "Refmap-A3-0-7-A.png",
    "Refmap-A3-1-0-A.png",
    "Refmap-A3-1-1-A.png",
    "Refmap-A3-1-2-A.png",
    "Refmap-A3-1-3-A.png",
    "Refmap-A3-1-4-A.png",
    "Refmap-A3-1-5-A.png",
    "Refmap-A3-1-6-A.png",
    "Refmap-A3-1-7-A.png",
    "Refmap-A3-2-0-A.png",
    "Refmap-A3-2-1-A.png",
    "Refmap-A3-2-2-A.png",
    "Refmap-A3-2-3-A.png",
    "Refmap-A3-2-4-A.png",
    "Refmap-A3-2-5-A.png",
    "Refmap-A3-2-6-A.png",
    "Refmap-A3-2-7-A.png",
    "Refmap-A3-3-0-A.png",
    "Refmap-A3-3-1-A.png",
    "Refmap-A3-3-2-A.png",
    "Refmap-A3-3-3-A.png",
    "Refmap-A3-3-4-A.png",
    "Refmap-A3-3-5-A.png",
    "Refmap-A3-3-6-A.png",
    "Refmap-A3-3-7-A.png",
    "Refmap-A4-0-0.png",
    "Refmap-A4-0-1.png",
    "Refmap-A4-0-2.png",
    "Refmap-A4-0-3.png",
    "Refmap-A4-0-4.png",
    "Refmap-A4-0-5.png",
    "Refmap-A4-0-6.png",
    "Refmap-A4-0-7.png",
    "Refmap-A4-1-0.png",
    "Refmap-A4-1-1.png",
    "Refmap-A4-1-2.png",
    "Refmap-A4-1-3.png",
    "Refmap-A4-1-4.png",
    "Refmap-A4-1-5.png",
    "Refmap-A4-1-6.png",
    "Refmap-A4-1-7.png",
    "Refmap-A4-2-0.png",
    "Refmap-A4-2-1.png",
    "Refmap-A4-2-2.png",
    "Refmap-A4-2-3.png",
    "Refmap-A4-2-4.png",
    "Refmap-A4-2-5.png",
    "Refmap-A4-2-6.png",
    "Refmap-A4-2-7.png",
    "Refmap-A4-3-0.png",
    "Refmap-A4-3-1.png",
    "Refmap-A4-3-2.png",
    "Refmap-A4-3-3.png",
    "Refmap-A4-3-4.png",
    "Refmap-A4-3-5.png",
    "Refmap-A4-3-6.png",
    "Refmap-A4-3-7.png",
    "Refmap-A4-4-0.png",
    "Refmap-A4-4-1.png",
    "Refmap-A4-4-2.png",
    "Refmap-A4-4-3.png",
    "Refmap-A4-4-4.png",
    "Refmap-A4-4-5.png",
    "Refmap-A4-4-6.png",
    "Refmap-A4-4-7.png",
    "Refmap-A4-5-0.png",
    "Refmap-A4-5-1.png",
    "Refmap-A4-5-2.png",
    "Refmap-A4-5-3.png",
    "Refmap-A4-5-4.png",
    "Refmap-A4-5-5.png",
    "Refmap-A4-5-6.png",
    "Refmap-A4-5-7.png")

}
