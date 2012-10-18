package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
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
extends TiledImageResource[Tileset, TilesetMetadata]
{
  import Tileset.tilesize
  def meta = Tileset
  
  def tileH = tilesize
  def tileW = tilesize
}

object Tileset extends MetaResource[Tileset, TilesetMetadata] {
  def rcType = "tileset"
  def keyExts = Array("png")
  
  def tilesize = 32
  def halftile = tilesize/2
  
  def defaultInstance(proj: Project, name: String) = 
    Tileset(proj, name, TilesetMetadata())
}

