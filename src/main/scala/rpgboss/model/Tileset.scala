package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class Tileset(name: ObjName, 
                   metadata: TilesetMetadata,
                   bytes: Array[Byte] = Array.empty) 
extends Resource
{
  import Tileset._
  
  def xPixels = metadata.xTiles*tilesize
  def yPixels = metadata.yTiles*tilesize
  
  def getImage = {
    // Draws image from bytes onto a new image of correct dimensions.
    val imgFromBytes = ImageIO.read(new ByteArrayInputStream(bytes))
    
    val imgWithRightDims = new BufferedImage(xPixels, yPixels, TYPE_4BYTE_ABGR)
    imgWithRightDims.getGraphics.drawImage(imgFromBytes, 0, 0, null)
  }
  
  def writeToDisk() = {
    name.dirFile.makeWriteableDir()
    
    metadata.writeTo(new FileOutputStream(name.metadataFile))
    
    Tileset.getDatafile(name).writeBytes(bytes)
  }
}

object Tileset extends MetaResource[Tileset] {
  def resourceType = "tileset"
  def displayName = "Tileset"
  def displayNamePlural = "Tilesets"
  
  val tilesize = 32
  
  def readFromDisk(name: ObjName) : Option[Tileset] = {
    val metadataFile = name.metadataFile
    
    if(metadataFile.canRead)
    {
      val metadata = 
        TilesetMetadata.parseFrom(new FileInputStream(metadataFile))
      
      val dataFile = getDatafile(name)
      val bytes = if(dataFile.canRead) dataFile.getBytes else Array.empty[Byte]
      
      Some(Tileset(name, metadata, bytes))
    }
    else None
  }
  
  def getDatafile(name: ObjName) = new File(name.dirFile, "imageset.png")
}

