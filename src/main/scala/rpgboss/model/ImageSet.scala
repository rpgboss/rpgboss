package rpgboss.model

import rpgboss.lib._

case class Tileset(name: ObjName, 
                   metadata: TilesetMetadata) 
extends Resource 
{
  def meta = Tileset
}

object Tileset extends MetaResource {
  def resourceType = "tileset"
  def displayName = "Tileset"
  def displayNamePlural = "Tilesets"
}

case class TilesetMetadata(val tilesize: Int, xTiles: Int, yTiles: Int)

