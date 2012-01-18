package rpgboss.model

import rpgboss.lib._
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
  
  val (tileH, tileW) = {
    val oneSprite = name(0) == '$'
    
    val tileH = img.height / 3 / (if(oneSprite) 1 else 2)
    val tileW = img.width  / 4 / (if(oneSprite) 1 else 4)
    
    (tileH, tileW)
  }
  
  
}

object Spriteset extends MetaResource[Spriteset, SpritesetMetadata] {
  def rcType = "spriteset"
  def keyExt = "png"
  
  def defaultInstance(proj: Project, name: String) = 
    Spriteset(proj, name, SpritesetMetadata())
}

