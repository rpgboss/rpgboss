package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import scala.collection.JavaConversions._

import java.io._

case class Autotile(proj: Project,
                    name: String,
                    passability: Short)
extends ImageResource[Autotile]
{
  def meta = Autotile
  
  def writeMetadataToFos(fos: FileOutputStream) =
    AutotileMetadata.newBuilder()
      .setPassability(passability)
      .build().writeTo(fos)
}

object Autotile extends MetaResource[Autotile] {
  def rcType = "autotile"
  def displayName = "Autotile"
  def displayNamePlural = "Autotiles"
  
  def defaultInstance(proj: Project, name: String) = 
    Autotile(proj, name, 0)
  
  def fromMetadata(proj: Project, name: String, fis: FileInputStream) = {
    val m = AutotileMetadata.parseFrom(fis)
    Autotile(proj, name, m.getPassability.toShort)
  }
}
