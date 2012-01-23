package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._
import java.io._
import java.util.Arrays

case class RpgMapMetadata(parent: Int,
                          title: String,
                          xSize: Int,
                          ySize: Int,
                          tilesets: List[String])

case class RpgMap(proj: Project, id: Int, metadata: RpgMapMetadata)
extends Resource[RpgMap, RpgMapMetadata]
{
  def meta = RpgMap
  def name = RpgMap.idToName(id)
  
  def saveMapData(d: RpgMapData) =
    d.writeToFile(proj, name)
  
  def readMapData() : Option[RpgMapData] = 
    RpgMapData.readFromDisk(proj, name)
}

object RpgMap extends MetaResource[RpgMap, RpgMapMetadata] {
  def rcType = "map"
  def keyExts = Array(metadataExt)
  
  def idToName(id: Int) = "Map%d".format(id)
  
  def apply(proj: Project, name: String, metadata: RpgMapMetadata) = 
    apply(proj, name.drop(3).toInt, metadata)
  
  def readFromDisk(proj: Project, id: Int) : RpgMap = 
    readFromDisk(proj, idToName(id))
    
  override def rcDir(proj: Project) = proj.mapsDir

  val initXSize = 20
  val initYSize = 15
  
  val bytesPerTile = 3
  
  val autotileByte : Byte = -2
  val emptyTileByte : Byte = -1
    
  def defaultInstance(proj: Project, name: String) : RpgMap = {
    val m = RpgMapMetadata(-1, "Starting Map",
                           initXSize, initYSize, 
                           List("Refmap-TileA5",
                                "Refmap-TileB",
                                "Refmap-TileC",
                                "Refmap-TileD",
                                "Refmap-TileE"))
    RpgMap(proj, name, m)
  }
  def defaultInstance(proj: Project, id: Int) : RpgMap = 
    defaultInstance(proj, idToName(id))
  
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
