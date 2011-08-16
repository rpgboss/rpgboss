package rpgboss.model

import rpgboss.lib._
import rpgboss.message._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import java.io._

case class Tileset(name: ObjName, 
                   metadata: TilesetMetadata,
                   bytes: Array[Byte] = Array.empty) 
extends Resource
{
  
  def writeToDisk() = {
    val dirFile = name.dirFile
    
    // make the directory
    dirFile.forceMkdirs()
    
    implicit val formats = Message.formats
    
    name.metadataFile.write(Serialization.write(metadata))
  }
}

object Tileset extends MetaResource[Tileset] {
  def resourceType = "tileset"
  def displayName = "Tileset"
  def displayNamePlural = "Tilesets"
  
  val tilesize = 32
  
  def readFromDisk(name: ObjName) : Option[Tileset] = {
    implicit val formats = Message.formats
    
    val metadataFile = name.metadataFile
    
    if(metadataFile.canRead)
    {
      val metadata = 
        Serialization.read[TilesetMetadata](new FileReader(metadataFile))
      
      val dataFile = getDataFile(name.dirFile) 
      val bytes = if(dataFile.canRead) dataFile.getBytes else Array.empty[Byte]
      
      Some(Tileset(name, metadata, bytes))
    }
    else None
  }
  
  def getDataFile(rcDir: File) = new File(rcDir, "imageset.png")
}

case class TilesetMetadata(xTiles: Int, yTiles: Int)

