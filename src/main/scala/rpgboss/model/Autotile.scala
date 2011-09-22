package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import scala.collection.JavaConversions._

import java.io._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

case class Autotile(proj: Project,
                    name: String,
                    passability: Short)
extends ImageResource[Autotile]
{
  import Tileset.tilesize
  def meta = Autotile
  
  def writeMetadataToFos(fos: FileOutputStream) =
    AutotileMetadata.newBuilder()
      .setPassability(passability)
      .build().writeTo(fos)
  
  def representativeImg = {
    getImg() map { diskImg =>
      val targetImage = 
        new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
    
      val g = targetImage.createGraphics()
      
      if(diskImg.getHeight == 3*tilesize) {
        g.drawImage(diskImg, 
                    0, 0, tilesize, tilesize,
                    0, 0, tilesize, tilesize,
                    null)
      } else {
        val halftile = tilesize/2
        g.drawImage(diskImg,
                    0, 0, halftile, halftile,
                    0, 0, halftile, halftile,
                    null)
        g.drawImage(diskImg,
                    halftile, 0, tilesize, halftile,
                    tilesize+halftile, 0, 2*tilesize, halftile,
                    null)
        g.drawImage(diskImg,
                    0, halftile, halftile, tilesize,
                    0, diskImg.getHeight-halftile, halftile, diskImg.getHeight,
                    null)
        g.drawImage(diskImg,
                    halftile, halftile, tilesize, tilesize,
                    diskImg.getWidth-halftile, diskImg.getHeight-halftile,
                    diskImg.getWidth, diskImg.getHeight,
                    null)
      }
      
      targetImage
      
    } getOrElse {
      val errImgStream = 
        getClass.getClassLoader.getResourceAsStream("error.png")
      
      ImageIO.read(errImgStream)
    }
  }
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
