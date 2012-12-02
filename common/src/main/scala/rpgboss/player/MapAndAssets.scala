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
class MapAndAssets(project: Project, mapName: String) {
  val map: RpgMap = RpgMap.readFromDisk(project, mapName)
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
  
  def getBlockedDirsOf(xTile: Int, yTile: Int): Byte = {
    import RpgMap._
    import Constants.DirectionMasks._
    val xIdx = xTile*bytesPerTile
    
    // Test top layer first, as if the top layer provides an answer, there is
    // no need to test subsequent layers
    for(layerAry <- List(mapData.topLayer, mapData.midLayer, mapData.botLayer))
    {
      val row = layerAry(yTile)
      val byte1 = row(xIdx)
      val byte2 = row(xIdx+1)
      val byte3 = row(xIdx+2)

      if(byte1 < 0) {
        // Empty tile or autotile
        if(byte1 == autotileByte) {
          return autotiles(byte2).metadata.blockedDirs
        } else { 
          // Do nothing... just continue with next layer
        }
      } else { // tileset tile
        return tilesets(byte1).metadata.blockedDirsAry(yTile)(xTile).toByte
      }
    }
    
    // It seems that all layers had an empty tile. Default to not blocked
    return NONE.toByte
  }
  
  /**
   * Test if there is a collision of a POINT moving from positions from 
   * (x0, y0) to (x0+dx, x0+dy).
   * 
   * However, this is only accurate if only one of dx or dy is nonzero.
   * This is because diagonal movement is implemented as small alternating
   * movements along x and y axis.
   * 
   * Generally speaking one will want to test the Collission of the four
   * corners of a bounding box, so use mapCollisionBox
   * 
   * @return  true if there is a collision, false if it's a pass
   */
  def mapCollisionPoint(x0: Float, y0: Float, dx: Float, dy: Float): Boolean = 
  {
    import RpgMap._
    import Constants.DirectionMasks._
    
    val xTile0 = x0.toInt
    val yTile0 = y0.toInt
    val xTile1 = (x0+dx).toInt
    val yTile1 = (y0+dy).toInt
    
    // Check if destination tile is within map. If not, it's a collision
    if(xTile1 < 0 || xTile1 >= map.metadata.xSize || 
        yTile1 < 0 || yTile1 >= map.metadata.ySize) {
      return true
    }
    
    // If the point doesn't even change which tile it's in, allow a pass.
    val changeTileX = xTile0 != xTile1
    val changeTileY = yTile0 != yTile1
    
    if(!changeTileX && !changeTileY) {
      return false
    } else {
      val blockedDirs0 = getBlockedDirsOf(xTile0, yTile0)
      val blockedDirs1 = getBlockedDirsOf(xTile1, yTile1)
      
      def isBlocked(blockMask: Byte, dir: Int) = (blockMask & dir) == dir
      
      if(changeTileX) {
        // Test x direction collision first
        if(dx > 0) {
          return isBlocked(blockedDirs0, EAST)||isBlocked(blockedDirs1, WEST)
        } else {
          return isBlocked(blockedDirs0, WEST)||isBlocked(blockedDirs1, EAST)
        }
      } else {
        // Test y direction collision next. 
        // (We've established that tile changes in at least one direction.)
        if(dy > 0) {
          return isBlocked(blockedDirs0, SOUTH)||isBlocked(blockedDirs1, NORTH)
        } else {
          return isBlocked(blockedDirs0, NORTH)||isBlocked(blockedDirs1, SOUTH)
        }
      } 
    }
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