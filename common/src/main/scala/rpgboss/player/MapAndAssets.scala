package rpgboss.player
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture.TextureFilter

/**
 * This class wraps a map and its assets. It should only be instantiated
 * on the Gdx thread, as it makes calls to OpenGL
 */
class MapAndAssets(project: Project, name: String) {
  val map: RpgMap = RpgMap.readFromDisk(project, project.data.startingLoc.map)
  val mapData: RpgMapData = map.readMapData().get
  
  
  /***
   * This section is all the stuff that finds the graphics and packs it into
   * texture atlases.
   */
  
  val packerTiles = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    
  val autotiles: Array[Autotile] = map.metadata.autotiles.map { name =>
    Autotile.readFromDisk(project, name)
  }
    
  // Pack all the autotiles
  autotiles.map { autotile =>
    val autotilePix = new Pixmap(
        Gdx.files.absolute(autotile.dataFile.getAbsolutePath()))
    
    packerTiles.pack("autotile/%s".format(autotile.name), autotilePix)

    autotilePix.dispose()
  }
  
  val tilesets = map.metadata.tilesets.map(
    name => Tileset.readFromDisk(project, name)).toArray[Tileset]
  
  // Pack all tilesets
  tilesets.map { tileset =>
    val tilesetPix = new Pixmap(
        Gdx.files.absolute(tileset.dataFile.getAbsolutePath()))
    
    packerTiles.pack("tileset/%s".format(tileset.name), tilesetPix)
    
    tilesetPix.dispose()
  }
  
  //info("Packed tilesets and autotiles into %d pages".format(
  //    packerTiles.getPages().size))
  
  // Generate texture atlas, nearest neighbor with no mipmaps
  val atlasTiles = packerTiles.generateTextureAtlas(
      TextureFilter.Nearest, TextureFilter.Nearest, false)
  
  def dispose() = {
    atlasTiles.dispose()
  }
}