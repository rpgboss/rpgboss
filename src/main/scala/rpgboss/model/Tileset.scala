package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._

import javax.imageio._

import java.io._
import java.awt.image._

case class TilesetMetadata(passabilities: Array[Byte] = Array.empty)

case class Tileset(proj: Project,
                   name: String, 
                   metadata: TilesetMetadata) 
extends ImageResource[Tileset, TilesetMetadata]
{
  import Tileset.tilesize
  def meta = Tileset
      
  def getTile(x: Int, y: Int) = imageOpt.map(img => {
    if(x < img.getWidth/tilesize && y < img.getHeight/tilesize)
      img.getSubimage(x*tilesize, y*tilesize, tilesize, tilesize)
    else 
      ImageResource.errorTile
  }).getOrElse(ImageResource.errorTile)
}

object Tileset extends MetaResource[Tileset, TilesetMetadata] {
  def rcType = "tileset"
  
  def tilesize = 32
  
  def defaultInstance(proj: Project, name: String) = 
    Tileset(proj, name, TilesetMetadata())
}

