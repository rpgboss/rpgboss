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
  import Autotile.DirectionMasks._
  def meta = Autotile
  
  def writeMetadataToFos(fos: FileOutputStream) =
    AutotileMetadata.newBuilder()
      .setPassability(passability)
      .build().writeTo(fos)
  
  def representativeImg() = {
    imageOpt map { img =>
      val targetImage = 
        new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
    
      val g = targetImage.createGraphics()
      
      if(img.getHeight == 3*tilesize) {
        g.drawImage(img, 
                    0, 0, tilesize, tilesize,
                    0, 0, tilesize, tilesize,
                    null)
      } else {
        val halftile = tilesize/2
        g.drawImage(img,
                    0, 0, halftile, halftile,
                    0, 0, halftile, halftile,
                    null)
        g.drawImage(img,
                    halftile, 0, tilesize, halftile,
                    tilesize+halftile, 0, 2*tilesize, halftile,
                    null)
        g.drawImage(img,
                    0, halftile, halftile, tilesize,
                    0, img.getHeight-halftile, halftile, img.getHeight,
                    null)
        g.drawImage(img,
                    halftile, halftile, tilesize, tilesize,
                    img.getWidth-halftile, img.getHeight-halftile,
                    img.getWidth, img.getHeight,
                    null)
      }
      
      targetImage
      
    } getOrElse {
      ImageResource.errorTile
    }
  }
  
  // autotileConfig must be a positive integer
  def getTile(autotileConfig: Int, frame: Byte) = imageOpt map { img =>
    require(autotileConfig >= 0, "Autotile config integer must be positive.")
    
    println("Autotile.getTile(%d, %d)".format(autotileConfig, frame))
    
    val tile = 
      new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
    
    val g = tile.createGraphics()
    val c = autotileConfig
    val ht = tilesize/2
    
    g.drawRect(5, 5, 5, 5)
    
    // draw corner helper method
    def drawCorner(srcXHt: Int, srcYHt: Int, destMask: Int) = {
      val destX = 
        if((destMask & NE) > 0 || (destMask & SE) > 0) ht else 0
      val destY =
        if((destMask & SE) > 0 || (destMask & SW) > 0) ht else 0
      
      g.drawImage(img,
                  destX, destY, destX+ht, destY+ht,
                  srcXHt*ht, srcYHt*ht, (srcXHt+1)*ht, (srcYHt+1)*ht,
                  null)
    }
    
    if(img.getHeight == 3*tilesize) {
      if(autotileConfig == 0xff)
        // completely isolated: draw isolated part
        g.drawImage(img, 0, 0, tilesize, tilesize, null)
      else {
        // draw center portion. We'll fill in edges after.
        g.drawImage(img, ht, ht*3, tilesize, tilesize, null)
        
        // handle edges and corners if exist
        if(c > 0) {
          // draw cardinal direction edges
          if((c & NORTH) > 0)
            g.drawImage(img,
                        0, 0, 2*ht, 1*ht,
                        1*ht, 2*ht, 3*ht, 3*ht, 
                        null)
          
          if((c & EAST) > 0)
            g.drawImage(img,
                        1*ht, 0, 2*ht, 2*ht,
                        3*ht, 3*ht ,4*ht, 5*ht,
                        null)
          
          if((c & SOUTH) > 0)
            g.drawImage(img,
                        0, 1*ht, 2*ht, 2*ht,
                        1*ht, 5*ht, 3*ht, 6*ht,
                        null)
          
          if((c & WEST) > 0)
            g.drawImage(img,
                        0, 0, 1*ht, 2*ht,
                        0, 3*ht, 1*ht, 5*ht,
                        null)
          
          def handleCorner(cardinalDirs: Int, ordinalDir: Int,
                           cornerXHt: Int, cornerYHt: Int,
                           inverseXHt: Int, inverseYHt: Int) = {
            if((c & cardinalDirs) == cardinalDirs)
              drawCorner(cornerXHt*ht, cornerYHt*ht, ordinalDir)
            else if((c & cardinalDirs) == 0 && (c & ordinalDir) > 0)
              drawCorner(inverseXHt*ht, inverseYHt*ht, ordinalDir)
          }
          
          handleCorner(NORTH|EAST, NE, 3, 2, 3, 0)
          handleCorner(SOUTH|EAST, SE, 3, 5, 3, 1)
          handleCorner(SOUTH|WEST, SW, 0, 5, 2, 1)
          handleCorner(NORTH|WEST, NW, 0, 2, 2, 0)    
        }
      }
    } else {
    }
    
    tile
  } getOrElse ImageResource.errorTile
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
  
  object DirectionMasks {
    val NORTH = 1 << 0
    val EAST  = 1 << 1
    val SOUTH = 1 << 2
    val WEST  = 1 << 3
    val NE    = 1 << 4
    val SE    = 1 << 5
    val SW    = 1 << 6
    val NW    = 1 << 7
  }
}
