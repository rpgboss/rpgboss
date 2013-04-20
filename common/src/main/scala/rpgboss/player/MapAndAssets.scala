package rpgboss.player
import rpgboss.model._
import rpgboss.model.Constants.DirectionMasks._
import rpgboss.model.resource._
import rpgboss.player.entity.Entity
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.Vector3

/**
 * This class wraps a map and its assets. It should only be instantiated
 * on the Gdx thread, as it makes calls to OpenGL
 */
class MapAndAssets(project: Project, mapName: String) {
  val map: RpgMap = RpgMap.readFromDisk(project, mapName)
  val mapData: RpgMapData = map.readMapData().get

  /**
   * *
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
    val xIdx = xTile * bytesPerTile

    // Test top layer first, as if the top layer provides an answer, there is
    // no need to test subsequent layers
    for (layerAry <- List(mapData.topLayer, mapData.midLayer, mapData.botLayer)) {
      val row = layerAry(yTile)
      val byte1 = row(xIdx)
      val byte2 = row(xIdx + 1)
      val byte3 = row(xIdx + 2)

      if (byte1 < 0) {
        // Empty tile or autotile
        if (byte1 == autotileByte) {
          val tiledata = autotiles(byte2).metadata

          if (tiledata.height == 0)
            return tiledata.blockedDirs
        } else {
          // Empty tile: Do nothing... just continue with next layer
        }
      } else { // tileset tile
        val tiledata = tilesets(byte1).metadata

        if (tiledata.heightAry(byte3)(byte2) == 0)
          return tiledata.blockedDirsAry(byte3)(byte2).toByte
      }
    }

    return NONE.toByte
  }

  /**
   * Test if there is a collision of a box with movement defined by |dx| and
   * |dy|. The re-route result will be for the bigger axis of |dx| and |dy|.
   * If they are equal, it will be for the |dx| axis.
   *
   * @return  (collision, reroute)  |collision| is true if there is a collision.
   *                                |reroute| returns which way the entity is
   *                                recommended to go if it wants to continue
   *                                along its path. For instance, for an entity
   *                                traveling in x-axis, a +1 reroute suggests
   *                                that it is blocked (probably by a corner)
   *                                but could continue by going in the
   *                                y-positive direction. Negative means going
   *                                in the y-negative direction, and a 0 means
   *                                there is no suggested rerouting.
   */
  def getCollisions(entity: Entity, x: Float, y: Float, dx: Float,
                    dy: Float): (Boolean, Int) = {
    if (dx == 0 && dy == 0)
      return (false, 0)

    val changeVector = new Vector3(dx, dy, 0)

    val boundingBox = entity.getBoundingBox()
    val minX = (boundingBox.getMin().x + dx).toInt
    val minY = (boundingBox.getMin().y + dy).toInt
    val maxX = (boundingBox.getMax().x + dx).toInt
    val maxY = (boundingBox.getMax().y + dy).toInt

    // 1. Test for going off map edge
    if (!map.metadata.withinBounds(minX, minY) ||
      !map.metadata.withinBounds(maxX, maxY))
      return (true, 0)

    var blocked = false
    // Being blocked in the positive direction adds -1.
    // Being blocked in the negative direction adds  1.
    var reroute = 0

    if (math.abs(dx) > math.abs(dy)) {
      if (dx >= 0) {
        if (flagged(getBlockedDirsOf(maxX, minY), WEST)) {
          blocked = true
          reroute += 1
        }
        if (flagged(getBlockedDirsOf(maxX, maxY), WEST)) {
          blocked = true
          reroute -= 1
        }
      } else {
        if (flagged(getBlockedDirsOf(minX, minY), EAST)) {
          blocked = true
          reroute += 1
        }
        if (flagged(getBlockedDirsOf(minX, maxY), EAST)) {
          blocked = true
          reroute -= 1
        }
      }
    } else {
      if (dy >= 0) {
        if (flagged(getBlockedDirsOf(minX, maxY), NORTH)) {
          blocked = true
          reroute += 1
        }
        if (flagged(getBlockedDirsOf(maxX, maxY), NORTH)) {
          blocked = true
          reroute -= 1
        }
      } else {
        if (flagged(getBlockedDirsOf(minX, minY), SOUTH)) {
          blocked = true
          reroute += 1
        }
        if (flagged(getBlockedDirsOf(maxX, minY), SOUTH)) {
          blocked = true
          reroute -= 1
        }
      }
    }

    return (blocked, reroute)
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