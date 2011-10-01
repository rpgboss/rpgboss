package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import scala.collection.JavaConversions._
import com.google.protobuf.ByteString

import java.io._

case class RpgMap(id: Int,
                  parent: Int,
                  name: String,
                  xSize: Int,
                  ySize: Int,
                  tilesets: Vector[String])
extends HasName
{
  def saveMetadata(p: Project) = {
    RpgMap.metadataFile(p, id).prepareWrite({ fos =>
      MapMetadata.newBuilder()
        .setId(id)
        .setParent(parent)
        .setName(name)
        .setXSize(xSize)
        .setYSize(ySize)
        .addAllTilesets(tilesets)
      .build()
      .writeTo(fos)
      
      true
    })
  }
  
  def saveMapData(p: Project, d: RpgMapData) =
    d.writeToFile(RpgMap.dataFile(p, id))
  
  def readMapData(p: Project) : Option[RpgMapData] = {
    val f = RpgMap.dataFile(p, id)
    if(f.canRead)
    {
      val serial = 
        MapDataSerial.parseFrom(new FileInputStream(f))
      
      Some(RpgMapData(serial.getBotLayer.toByteArray,
                      serial.getMidLayer.toByteArray,
                      serial.getTopLayer.toByteArray))
    }
    else None
  }
    
}

// this class has mutable members
case class RpgMapData(botLayer: Array[Byte],
                      midLayer: Array[Byte],
                      topLayer: Array[Byte])
{
  def writeToFile(f: File) = f.prepareWrite({ fos =>
    MapDataSerial.newBuilder()
      .setBotLayer(ByteString.copyFrom(botLayer))
      .setMidLayer(ByteString.copyFrom(midLayer))
      .setTopLayer(ByteString.copyFrom(topLayer))
    .build()
    .writeTo(fos)
    
    true
  })
  
  def drawOrder = List(botLayer, midLayer, topLayer)
}

object RpgMap {
  def mapsDir(p: Project) = new File(p.projectDir, "maps")
  
  def metadataExt = "rpgmapmeta"
  
  def metadataFile(p: Project, id: Int) = 
    new File(mapsDir(p), "Map%d.%s".format(id, metadataExt))
  def dataFile(p: Project, id: Int) = 
    new File(mapsDir(p), "Map%d.rpgmapdata".format(id))
  
  def readMetadata(f: File) : Option[RpgMap] = {
    if(f.canRead)
    {
      val serial = 
        MapMetadata.parseFrom(new FileInputStream(f))
      
      Some(RpgMap(serial.getId, serial.getParent, serial.getName,
                  serial.getXSize, serial.getYSize, 
                  Vector.empty ++ serial.getTilesetsList))
    }
    else None
  }

  val initXSize = 20
  val initYSize = 15
  
  val bytesPerTile = 3
  
  val autotileByte : Byte = -2
  val emptyTileByte : Byte = -1
    
  def defaultMap = {
    RpgMap(1, -1, "Starting Map", 
           initXSize, initYSize, 
           Vector("Refmap-TileA5",
                  "Refmap-TileB",
                  "Refmap-TileC",
                  "Refmap-TileD",
                  "Refmap-TileE"))
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
    
    RpgMapData(autoLayer, emptyLayer, emptyLayer)
  }
  
  def defaultMapData = emptyMapData(initXSize, initYSize)
  
}
