package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import com.google.protobuf.ByteString

import scala.collection.JavaConversions._

import javax.imageio._

import java.io._
import java.awt.image._

case class Tileset(proj: Project,
                   name: String, 
                   passabilities: Array[Byte]) 
extends ImageResource[Tileset]
{
  def meta = Tileset
  
  def writeMetadataToFos(fos: FileOutputStream) =
    TilesetMetadata.newBuilder()
      .setPassabilities(ByteString.copyFrom(passabilities))
      .build().writeTo(fos)
}

object Tileset extends MetaResource[Tileset] {
  def rcType = "tileset"
  def displayName = "Tileset"
  def displayNamePlural = "Tilesets"
  
  def tilesize = 32
  
  def defaultInstance(proj: Project, name: String) = 
    Tileset(proj, name, Array.empty)
  
  def fromMetadata(proj: Project, name: String, fis: FileInputStream) = {
    val m = TilesetMetadata.parseFrom(fis)
    Tileset(proj, name, m.getPassabilities.toByteArray)
  }
}

