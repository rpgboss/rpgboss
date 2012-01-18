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
  
  lazy val terrainMode = img.height == 3*tilesize
  
  private def draw(g: Graphics2D,
                   srcXHt: Int, srcYHt: Int, destXHt: Int, destYHt: Int,
                   widthHt: Int, heightHt: Int) =
  {
    val ht = tilesize/2
    val sourceSlice = img.subimage(
      srcXHt*ht, srcYHt*ht, widthHt*ht, heightHt*ht)
    
    g.drawImage(sourceSlice, destXHt*ht, destYHt*ht, null)
  }
  
  // draw corner helper method
  // htXOffset offsets the x axis in halftiles (for multiframe tiles)
  def drawCorner(g: Graphics2D, htXOffset: Int, 
                 srcXHt: Int, srcYHt: Int, destMask: Int) = 
  {
    val destXHt = if((destMask & NE) > 0 || (destMask & SE) > 0) 1 else 0
    val destYHt = if((destMask & SE) > 0 || (destMask & SW) > 0) 1 else 0
    
    draw(g, htXOffset+srcXHt, srcYHt, destXHt, destYHt, 1, 1)
  }
  
  def isolatedImg() = {
    val targetImage = 
      new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_4BYTE_ABGR)
  
    val g = targetImage.createGraphics()
    
    if(terrainMode)
      draw(g, 0, 0, 0, 0, 2, 2)
    else {
      drawCorner(g, 0, 0, 0, NW)
      drawCorner(g, 0, 3, 0, NE)
      drawCorner(g, 0, 0, 3, SW)
      drawCorner(g, 0, 3, 3, SE)
    }
    
    targetImage
  }
  
  // autotileConfig must be a positive integer
  def getTile(autotileConfig: Int, frame: Byte) = { 
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
      
      // htXOffset offsets the x axis in halftiles (for multiframe tiles)
      def drawCardinalEdges(htXOffset: Int, yOffset: Int) = {
        // draw cardinal direction edges
        if((c & NORTH) > 0) draw(g, htXOffset+1, yOffset+0, 0, 0, 2, 1)
        if((c & EAST) > 0)  draw(g, htXOffset+3, yOffset+1, 1, 0, 1, 2)
        if((c & SOUTH) > 0) draw(g, htXOffset+1, yOffset+3, 0, 1, 2, 1)
        if((c & WEST) > 0)  draw(g, htXOffset+0, yOffset+1, 0, 0, 1, 2)
      }
      
      if(terrainMode) {
        val frameWidth = 2*tilesize
        val availableFrames = img.width / frameWidth
        val frameIdx = frame % availableFrames
        
        // how many half tiles to offset to get the right frame
        val frameHtXOffset = 4*frameIdx
        
        // draw center portion. We'll fill in edges after.
        // third argument does X offset to get to the correct frame
        draw(g, frameHtXOffset+1, 3, 0, 0, 2, 2)
        
        // handle edges and corners if exist
        if(c > 0) {
          drawCardinalEdges(frameHtXOffset, 2)
          
          def handleCorner(cardinalDirs: Int, ordinalDir: Int,
                           cornerXHt: Int, cornerYHt: Int,
                           inverseXHt: Int, inverseYHt: Int) = {
            if((c & cardinalDirs) == cardinalDirs)
              drawCorner(g, frameHtXOffset, cornerXHt, cornerYHt, ordinalDir)
            else if((c & cardinalDirs) == 0 && (c & ordinalDir) > 0)
              drawCorner(g, frameHtXOffset, inverseXHt, inverseYHt, ordinalDir)
          }
          
          handleCorner(NORTH|EAST, NE, 3, 2, 3, 0)
          handleCorner(SOUTH|EAST, SE, 3, 5, 3, 1)
          handleCorner(SOUTH|WEST, SW, 0, 5, 2, 1)
          handleCorner(NORTH|WEST, NW, 0, 2, 2, 0)    
        }
      } else {
        draw(g, 1, 1, 0, 0, 2, 2) // draw whole center portion
        
        if(c > 0) {
          drawCardinalEdges(0, 0)
          
          def handleCorner(cardinalDirs: Int, ordinalDir: Int,
                           cornerXHt: Int, cornerYHt: Int) = {
            if((c & cardinalDirs) == cardinalDirs)
              drawCorner(g, 0, cornerXHt, cornerYHt, ordinalDir)
          }
          
          handleCorner(NORTH|EAST, NE, 3, 0)
          handleCorner(SOUTH|EAST, SE, 3, 3)
          handleCorner(SOUTH|WEST, SW, 0, 3)
          handleCorner(NORTH|WEST, NW, 0, 0)    
        }
      }
      
      tile
    }
  }
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
