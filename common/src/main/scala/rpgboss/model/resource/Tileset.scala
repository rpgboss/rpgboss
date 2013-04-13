package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.FileHelper._

import org.json4s.native.Serialization

import scala.collection.JavaConversions._

import javax.imageio._

import java.io._
import java.awt.image._

/**
 * @param blockedDirsAry  Array of blocked directions. Should be a Byte,
 *                        but lift-json doesn't support that. Fuck it.
 */
case class TilesetMetadata(
  blockedDirsAry: Array[Array[Int]],
  heightAry: Array[Array[Int]]) {

  // Constructor to port legacy data
  def this(blockedDirsAry: Array[Array[Int]]) = {
    this(blockedDirsAry, blockedDirsAry.map(a => new Array[Int](a.length)))
  }
}

case class Tileset(proj: Project,
                   name: String,
                   metadata: TilesetMetadata)
  extends TiledImageResource[Tileset, TilesetMetadata] {
  import Tileset.tilesize
  def meta = Tileset

  def tileH = tilesize
  def tileW = tilesize
  val xTiles = img.getWidth() / tileW
  val yTiles = img.getHeight() / tileH
}

object Tileset extends MetaResource[Tileset, TilesetMetadata] {
  def rcType = "tileset"
  def keyExts = Array("png")

  def tilesize = 32
  def halftile = tilesize / 2

  def defaultInstance(proj: Project, name: String) = {
    val tilesetWOMetadata = Tileset(proj, name,
      new TilesetMetadata(Array.empty))

    import Constants.DirectionMasks._

    // Generate blockedDirs array
    val x = tilesetWOMetadata.xTiles
    val y = tilesetWOMetadata.yTiles
    val blockedDirsAry = Array.fill(x, y)(NONE)
    val heightAry = Array.fill(x, y)(0)

    tilesetWOMetadata.copy(metadata =
      TilesetMetadata(blockedDirsAry, heightAry))
  }
}

