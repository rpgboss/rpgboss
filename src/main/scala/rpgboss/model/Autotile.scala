package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import com.weiglewilczek.slf4s.Logging

import scala.collection.JavaConversions._

import java.io._
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import javax.imageio.ImageIO

case class AutotileMetadata(passability: Short = 0)

case class Autotile(proj: Project,
                    name: String,
                    metadata: AutotileMetadata)
extends ImageResource[Autotile, AutotileMetadata] with Logging
{
  import Tileset.tilesize
  import Autotile.DirectionMasks._
  def meta = Autotile
  
  lazy val terrainMode = 
    imageOpt.map(_.getHeight == 3*tilesize).getOrElse(false)
  
  private def draw(g: Graphics2D, srcImg: BufferedImage,
                   srcXHt: Int, srcYHt: Int, destXHt: Int, destYHt: Int,
                   widthHt: Int, heightHt: Int) =
  {
    val ht = tilesize/2
    g.drawImage(srcImg,
                destXHt*ht, destYHt*ht, 
                (destXHt+widthHt)*ht, (destYHt+heightHt)*ht,
                srcXHt*ht, srcYHt*ht, 
                (srcXHt+widthHt)*ht, (srcYHt+heightHt)*ht,
                null)
  }
  
  // draw corner helper method
  def drawCorner(g: Graphics2D, srcImg: BufferedImage, 
                 srcXHt: Int, srcYHt: Int, destMask: Int) = 
  {
    val destXHt = if((destMask & NE) > 0 || (destMask & SE) > 0) 1 else 0
    val destYHt = if((destMask & SE) > 0 || (destMask & SW) > 0) 1 else 0
    
    draw(g, srcImg, srcXHt, srcYHt, destXHt, destYHt, 1, 1)
  }
  
  def isolatedImg() = {
    imageOpt map { img =>
      val targetImage = 
        new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
    
      val g = targetImage.createGraphics()
      
      if(terrainMode)
        draw(g, img, 0, 0, 0, 0, 2, 2)
      else {
        drawCorner(g, img, 0, 0, NW)
        drawCorner(g, img, 3, 0, NE)
        drawCorner(g, img, 0, 3, SW)
        drawCorner(g, img, 3, 3, SE)
      }
      
      targetImage
      
    } getOrElse {
      ImageResource.errorTile
    }
  }
  
  // autotileConfig must be a positive integer
  def getTile(autotileConfig: Int, frame: Byte) = imageOpt map { img =>
    require(autotileConfig >= 0, "Autotile config integer must be positive.")
    require(frame >= 0, "Frame byte must be positive.")
    
    logger.info("getTile (%d, %d) - %s".format(autotileConfig, frame, name))
    
    if(autotileConfig == 0xff) {
      // completely isolated
      isolatedImg()
    } else {
    
      val tile = 
        new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
      
      val g = tile.createGraphics()
      val c = autotileConfig
      val ht = tilesize/2
          
      def drawCardinalEdges(srcImg: BufferedImage, yOffset: Int) = {
        // draw cardinal direction edges
        if((c & NORTH) > 0) draw(g, srcImg, 1, 0+yOffset, 0, 0, 2, 1)
        if((c & EAST) > 0)  draw(g, srcImg, 3, 1+yOffset, 1, 0, 1, 2)
        if((c & SOUTH) > 0) draw(g, srcImg, 1, 3+yOffset, 0, 1, 2, 1)
        if((c & WEST) > 0)  draw(g, srcImg, 0, 1+yOffset, 0, 0, 1, 2)
      }
      
      if(terrainMode) {
        val framewidth = 2*tilesize
        val availableFrames = img.getWidth / framewidth
        val frameIdx = frame % availableFrames
        
        val frameImg = 
          img.getSubimage(framewidth*frameIdx, 0, framewidth, img.getHeight)
        
        // draw center portion. We'll fill in edges after.
        draw(g, frameImg, 1, 3, 0, 0, 2, 2)
        
        // handle edges and corners if exist
        if(c > 0) {
          drawCardinalEdges(frameImg, 2)
          
          def handleCorner(cardinalDirs: Int, ordinalDir: Int,
                           cornerXHt: Int, cornerYHt: Int,
                           inverseXHt: Int, inverseYHt: Int) = {
            if((c & cardinalDirs) == cardinalDirs)
              drawCorner(g, frameImg, cornerXHt, cornerYHt, ordinalDir)
            else if((c & cardinalDirs) == 0 && (c & ordinalDir) > 0)
              drawCorner(g, frameImg, inverseXHt, inverseYHt, ordinalDir)
          }
          
          handleCorner(NORTH|EAST, NE, 3, 2, 3, 0)
          handleCorner(SOUTH|EAST, SE, 3, 5, 3, 1)
          handleCorner(SOUTH|WEST, SW, 0, 5, 2, 1)
          handleCorner(NORTH|WEST, NW, 0, 2, 2, 0)    
        }
      } else {
        draw(g, img, 1, 1, 0, 0, 2, 2) // draw whole center portion
        
        if(c > 0) {
          drawCardinalEdges(img, 0)
          
          def handleCorner(cardinalDirs: Int, ordinalDir: Int,
                           cornerXHt: Int, cornerYHt: Int) = {
            if((c & cardinalDirs) == cardinalDirs)
              drawCorner(g, img, cornerXHt, cornerYHt, ordinalDir)
          }
          
          handleCorner(NORTH|EAST, NE, 3, 0)
          handleCorner(SOUTH|EAST, SE, 3, 3)
          handleCorner(SOUTH|WEST, SW, 0, 3)
          handleCorner(NORTH|WEST, NW, 0, 0)    
        }
      }
      
      tile
    }
  } getOrElse ImageResource.errorTile
}

object Autotile extends MetaResource[Autotile, AutotileMetadata] {
  def rcType = "autotile"
  def keyExt = "png"
  
  def defaultInstance(proj: Project, name: String) = 
    Autotile(proj, name, AutotileMetadata())
  
  // The mask is ON if that direction contains a tile DIFFERENT from
  // the current autotile type
  object DirectionMasks {
    val NORTH = 1 << 0
    val EAST  = 1 << 1
    val SOUTH = 1 << 2
    val WEST  = 1 << 3
    val NE    = 1 << 4
    val SE    = 1 << 5
    val SW    = 1 << 6
    val NW    = 1 << 7
    
    val offsets = Map(
      NORTH->(0, -1),
      EAST ->(1, 0),
      SOUTH->(0, 1),
      WEST ->(-1, 0),
      NE   ->(1, -1),
      SE   ->(1, 1),
      SW   ->(-1, 1),
      NW   ->(-1, -1)
    )
  } 
}
