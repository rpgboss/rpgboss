package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._
import java.io._

case class RpgMapMetadata(parent: String,
                          title: String,
                          xSize: Int,
                          ySize: Int,
                          tilesets: Vector[String])

case class RpgMap(proj: Project, name: String, metadata: RpgMapMetadata)
extends Resource[RpgMap, RpgMapMetadata]
{
  def meta = RpgMap
  
  def saveMapData(d: RpgMapData) =
    d.writeToFile(proj, name)
  
  def readMapData() : Option[RpgMapData] = 
    RpgMapData.readFromDisk(proj, name)
}

object RpgMap extends MetaResource[RpgMap, RpgMapMetadata] {
  def rcType = "map"
  
  def metadataExt = "mapmeta.json"
  
  override def metadataFile(p: Project, name: String) = 
    new File(p.mapsDir, "%s.%s".format(name, metadataExt))

  val initXSize = 20
  val initYSize = 15
  
  val bytesPerTile = 3
  
  val autotileByte : Byte = -2
  val emptyTileByte : Byte = -1
    
  def defaultInstance(proj: Project, name: String) = {
    val m = RpgMapMetadata("", "Starting Map", 
                           initXSize, initYSize, 
                           Vector("Refmap-TileA5",
                                  "Refmap-TileB",
                                  "Refmap-TileC",
                                  "Refmap-TileD",
                                  "Refmap-TileE"))
    RpgMap(proj, name, m)
  }
  
  def emptyMapData(xSize: Int, ySize: Int) = {
    val dataArySize = xSize*ySize*bytesPerTile
    val autoLayer  = {
      val a = Array[Byte](autotileByte,0,0)
      Array.tabulate[Byte](dataArySize)(i => a(i%a.length))
    }
    val emptyLayer = { 
      val a = Array[Byte](emptyTileByte,0,0)
      Array.tabulate[Byte](dataArySize)(i => a(i%a.length))
    }
    
    RpgMapData(autoLayer, emptyLayer, emptyLayer, Array.empty)
  }
  
  def dataIndex(x: Int, y: Int, xSize: Int) = (y*xSize+x)*bytesPerTile
  
  def defaultMapData = emptyMapData(initXSize, initYSize)
  
}
