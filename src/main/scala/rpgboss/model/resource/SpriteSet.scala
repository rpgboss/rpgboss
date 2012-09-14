package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._

import javax.imageio._

import java.io._
import java.awt.image._

case class SpritesetMetadata(boundsX: Int = Tileset.tilesize, 
                             boundsY: Int = Tileset.tilesize)

case class Spriteset(proj: Project,
                     name: String, 
                     metadata: SpritesetMetadata) 
extends TiledImageResource[Spriteset, SpritesetMetadata]
{
  def meta = Spriteset
  
  /**
   * Gets the size of the sprites
   */
  val (tileH, tileW) = {
    val oneSprite = name(0) == '$'
    
    val tileH = img.height / 4 / (if(oneSprite) 1 else 2)
    val tileW = img.width  / 3 / (if(oneSprite) 1 else 4)
    
    (tileH, tileW)
  }
  
  /**
   * Gets the offset for a given sprite in pixels. (texels if used as texture)
   * 
   * @param index   Given between 0-7. The sprite number in the page.
   * @param dir     One of Spriteset.DirectionOffsets. 0-3
   * @param step    One of Spriteset.Steps. 0-3
   */
  def srcTexels(index: Int, dir: Int, step: Int) = {
    // In units of sprites. tileW, tileH
    val xOffset = (index % 4)*(3)
    val yOffset = (index / 4)*(4)
    
    val xPix = (xOffset+step)*tileW
    val yPix = (yOffset+dir)*tileH
    
    (xPix, yPix)
  }
  
  /**
   * Given the position we want to sprite to be. Give the origin and size of
   * the sprite reasonably. If we specify the sprite to be at the center of a 
   * tile, i.e., at (2.5, 7.5), the sprite's bottom should be at 8.0, since we
   * want the bottom of the sprite to be at the bottom of the tile...
   * 
   * Note that the destination's "origin" is the top-left of the sprite, since
   * we have flipped the y-axis in libgdx.
   */
  def dstPosition(posX: Float, posY: Float) : (Float, Float, Float, Float) = {
    val dstWTiles = tileW.toFloat/Tileset.tilesize.toFloat
    val dstHTiles = tileH.toFloat/Tileset.tilesize.toFloat
    val dstOriginX = posX - dstWTiles/2.0f
    val dstOriginY = posY - dstHTiles + 0.5f
    
    (dstOriginX, dstOriginY, dstWTiles, dstHTiles)
  }
}

object Spriteset extends MetaResource[Spriteset, SpritesetMetadata] {
  def rcType = "spriteset"
  def keyExts = Array("png")
  
  def defaultInstance(proj: Project, name: String) = 
    Spriteset(proj, name, SpritesetMetadata())
  
  object DirectionOffsets {
    val SOUTH = 0
    val WEST  = 1
    val EAST  = 2
    val NORTH = 3
  }
  
  object Steps {
    val STEP1 = 0
    val STEP2 = 2
    val STILL = 1
  }
}

