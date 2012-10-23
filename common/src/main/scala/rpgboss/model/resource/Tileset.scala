package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._

import javax.imageio._

import java.io._
import java.awt.image._

/**
 * @param blockedDirsAry  Array of blocked directions. Should be a Byte,
 *                        but lift-json doesn't support that. Fuck it.
 */
case class TilesetMetadata(blockedDirsAry: Array[Array[Int]])

case class Tileset(proj: Project,
                   name: String, 
                   metadata: TilesetMetadata) 
extends TiledImageResource[Tileset, TilesetMetadata]
{
  import Tileset.tilesize
  def meta = Tileset
  
  def tileH = tilesize
  def tileW = tilesize
  val xTiles = img.getWidth()/tileW
  val yTiles = img.getHeight()/tileH
}

object Tileset extends MetaResource[Tileset, TilesetMetadata] {
  def rcType = "tileset"
  def keyExts = Array("png")
  
  def tilesize = 32
  def halftile = tilesize/2
  
  def defaultInstance(proj: Project, name: String) = {
    val tilesetWOMetadata = Tileset(proj, name, TilesetMetadata(Array.empty))
    
    import Constants.DirectionMasks._
    
    // Generate blockedDirs array
    val row = Array.fill(tilesetWOMetadata.xTiles)(NONE)
    val blockedDirs = Array.tabulate(tilesetWOMetadata.yTiles)(i => row.clone())
    
    tilesetWOMetadata.copy(metadata = TilesetMetadata(blockedDirs))    
  }
}

