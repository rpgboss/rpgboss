package rpgboss.editor.cache

import rpgboss.model._
import rpgboss.model.resource._

class AssetCache(proj: Project) {
  var tilesetMap = Map[String, Tileset]()
  var autotileMap = Map[String, Autotile]()

  def invalidate() = {
    val tilesets  = Tileset.list(proj).map(Tileset.readFromDisk(proj, _))
    val autotiles = Autotile.list(proj).map(Autotile.readFromDisk(proj, _))

    tilesetMap = Map(tilesets.map(t => t.name->t): _*)
    autotileMap = Map(autotiles.map(a => a.name->a): _*)
  }
  
  // Invalidate initally to load it all
  invalidate()
  
}