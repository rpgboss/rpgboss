package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import scala.collection.JavaConversions._
import com.google.protobuf.ByteString

import java.io._

case class RpgMapMetadata(id: Int,
                          parent: Int,
                          name: String,
                          xSize: Int,
                          ySize: Int,
                          tilesets: Vector[String])
extends HasName
{
  def writeToDisk(project: Project) = {
    RpgMap.metadataFile(project, id).prepareWrite({ fos =>
      MapMetadataSerial.newBuilder()
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
}

case class RpgMap(metadata: RpgMapMetadata, 
                  botLayer: Array[Byte],
                  midLayer: Array[Byte],
                  topLayer: Array[Byte])
{
  def writeToDisk(p: Project) = 
    metadata.writeToDisk(p) && 
    RpgMap.dataFile(p, metadata.id).prepareWrite({ fos =>
    MapDataSerial.newBuilder()
      .setBotLayer(ByteString.copyFrom(botLayer))
      .setMidLayer(ByteString.copyFrom(midLayer))
      .setTopLayer(ByteString.copyFrom(topLayer))
    .build()
    .writeTo(fos)
    
    true
  })
}

object RpgMap {
  def mapsDir(project: Project) = new File(project.metadata.projectDir, "maps")
  
  def metadataFile(project: Project, id: Int) = 
    new File(mapsDir(project), "Map%d.rpgmapmeta".format(id))
  def dataFile(project: Project, id: Int) = 
    new File(mapsDir(project), "Map%d.rpgmapdata".format(id))
    
  def firstMap = {
    val initXSize = 20
    val initYSize = 15
    val metadata = RpgMapMetadata(1, -1, "Starting Map", 
                                  initXSize, initYSize, Vector("town"))
    
    val arySize = 20*15*bytesPerTile
    val defaultLayerData = Array.fill[Byte](arySize)(0)
    RpgMap(metadata, defaultLayerData, defaultLayerData, defaultLayerData)
  }
  
  def bytesPerTile = 2
}
