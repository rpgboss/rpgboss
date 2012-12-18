package rpgboss.editor.cache

import rpgboss.model._
import rpgboss.model.resource._

class AssetCache(proj: Project) {
  private var tilesetMap = Map[String, Tileset]()
  private var autotileMap = Map[String, Autotile]()

  private def invalidate() = {
    val tilesets  = Tileset.list(proj).map(Tileset.readFromDisk(proj, _))
    val autotiles = Autotile.list(proj).map(Autotile.readFromDisk(proj, _))

    tilesetMap = Map(tilesets.map(t => t.name->t): _*)
    autotileMap = Map(autotiles.map(a => a.name->a): _*)
  }
  
  def getTileset(name: String) = tilesetMap(name)
  def getAutotile(name: String) = autotileMap(name)
  def getSpriteset(name: String) = 
    Spriteset.readFromDisk(proj, name)
  
  // Invalidate initally to load it all
  invalidate()
  
}