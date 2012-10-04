package rpgboss.model.resource

import rpgboss.model._
import rpgboss.lib._
import rpgboss.lib.FileHelper._
import net.liftweb.json.Serialization
import scala.collection.JavaConversions._
import java.io._
import java.util.Arrays
import net.liftweb.json.DefaultFormats

case class RpgMapMetadata(parent: String,
                          title: String,
                          xSize: Int,
                          ySize: Int,
                          tilesets: List[String]) {
}

case class RpgMap(proj: Project, name: String, metadata: RpgMapMetadata)
  extends Resource[RpgMap, RpgMapMetadata]
{
  def meta = RpgMap
  
  def saveMapData(d: RpgMapData) =
    d.writeToFile(proj, name)
  
  def readMapData() : Option[RpgMapData] = 
    RpgMapData.readFromDisk(proj, name)
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
  def mapExt = "mapdata.json"
  def keyExts = Array(mapExt)
  
  val initXSize = 40
  val initYSize = 30
  
  val bytesPerTile = 3
  
  val autotileByte : Byte = -2
  val emptyTileByte : Byte = -1

  def generateName(id: Int) = 
    "Map%06d.%s".formatLocal(java.util.Locale.US, id, mapExt)
  
  def defaultInstance(proj: Project, name: String) : RpgMap = { 
    val m = RpgMapMetadata("", "Starting Map",
                       initXSize, initYSize, 
                       List("Refmap-TileA5.png",
                            "Refmap-TileB.png",
                            "Refmap-TileC.png",
                            "Refmap-TileD.png",
                            "Refmap-TileE.png"))
    RpgMap(proj, name , m)
  }
  
  def emptyMapData(xSize: Int, ySize: Int) = {
    val autoLayer  = {
      // Generate a 3-byte tile
      val a = Array[Byte](autotileByte,0,0)
      // Make a whole row of that
      val row = Array.tabulate[Byte](xSize*bytesPerTile)(i => a(i%a.length))
      // Make multiple rows
      Array.tabulate[Array[Byte]](ySize)(i => row.clone())
    }
    val emptyLayer = { 
      val a = Array[Byte](emptyTileByte,0,0)
      val row = Array.tabulate[Byte](xSize*bytesPerTile)(i => a(i%a.length))
      Array.tabulate[Array[Byte]](ySize)(i => row.clone())
    }
    
    RpgMapData(autoLayer, emptyLayer, emptyLayer, Array.empty)
  }
  
  def defaultMapData = emptyMapData(initXSize, initYSize)
  
}
