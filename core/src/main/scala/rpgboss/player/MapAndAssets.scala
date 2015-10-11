package rpgboss.player
import rpgboss.model._
import rpgboss.model.DirectionMasks._
import rpgboss.model.resource._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture.TextureFilter
import scala.collection.mutable.ArrayBuffer

/**
 * This class wraps a map and its assets. It should only be instantiated
 * on the Gdx thread, as it makes calls to OpenGL
 */
class MapAndAssets(
  project: Project,
  val mapName: String,
  renderingOffForTesting: Boolean) {
  val map: RpgMap = RpgMap.readFromDisk(project, mapName)
  val mapData: RpgMapData = map.readMapData().get
  mapData.sanitizeForMetadata(map.metadata)

  private var _battleBackground = map.metadata.battleBackground
  private var _battleMusic = map.metadata.battleMusic
  private var _randomEncounterSettings = map.metadata.randomEncounterSettings
  def battleBackground = _battleBackground
  def battleMusic = _battleMusic
  def randomEncounterSettings = _randomEncounterSettings

  def setOverrideBattleSettings(battleBackground: String, battleMusic: String,
      battleMusicVolume: Float, randomEncountersOn: Boolean) = {
    _battleBackground =
      if (battleBackground.isEmpty())
        map.metadata.battleBackground
      else
        battleBackground
    _battleMusic =
      if (battleMusic.isEmpty())
        map.metadata.battleMusic
      else
        Some(SoundSpec(battleMusic, battleMusicVolume))
    _randomEncounterSettings =
      if (randomEncountersOn)
        map.metadata.randomEncounterSettings
      else
        RandomEncounterSettings()
  }

  private var _lastBattleX: Float = -1
  private var _lastBattleY: Float = -1
  def setLastBattlePosition(x: Float, y: Float) = {
    _lastBattleX = x
    _lastBattleY = y
  }

  def manhattanDistanceFromLastBattle(x: Float, y: Float) = {
    math.abs(x - _lastBattleX) + math.abs(y - _lastBattleY)
  }

  /**
   * *
   * This section is all the stuff that finds the graphics and packs it into
   * texture atlases.
   */

  val packerTiles =
    new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)

  val autotiles: Array[Autotile] = map.metadata.autotiles.map { name =>
    Autotile.readFromDisk(project, name)
  }

  // Pack all the autotiles
  autotiles.map { autotile =>
    val autotilePix = new Pixmap(autotile.getGdxFileHandle)

    packerTiles.pack("autotile/%s".format(autotile.name), autotilePix)

    autotilePix.dispose()
  }

  val tilesets = map.metadata.tilesets.map(
    name => Tileset.readFromDisk(project, name)).toArray[Tileset]

  // Pack all tilesets
  tilesets.map { tileset =>
    val tilesetPix = new Pixmap(tileset.getGdxFileHandle)

    packerTiles.pack("tileset/%s".format(tileset.name), tilesetPix)

    tilesetPix.dispose()
  }

  /**
   * Create a list of elevated tiles that may need to be drawn above the
   * player and event sprites.
   */
  case class ElevatedTile(tileX: Int, tileY: Int, byte1: Byte, byte2: Byte,
                          byte3: Byte, zPriority: Int)
  val elevatedTiles: Array[ElevatedTile] = {
    val buffer = new ArrayBuffer[ElevatedTile]

    for (
      layerAry <- List(mapData.botLayer, mapData.midLayer, mapData.topLayer)
    ) {
      for (tileY <- 0 until map.metadata.ySize) {
        val row = layerAry(tileY)
        import RpgMap.bytesPerTile
        for (tileX <- 0 until map.metadata.xSize) {
          val idx = tileX * bytesPerTile
          val byte1 = row(idx)
          val byte2 = row(idx + 1)
          val byte3 = row(idx + 2)

          if (byte1 < 0) {
            if (byte1 == RpgMap.autotileByte) { // Autotile
              val autotile = autotiles(byte2)
              val height = autotile.metadata.height.toInt
              if (height > 0) {
                buffer.append(ElevatedTile(
                  tileX, tileY, byte1, byte2, byte3, height + tileY))
              }
            }
          } else { // Regular tile
            val tileset = tilesets(byte1)
            val height = tileset.metadata.heightAry(byte3)(byte2)
            if (height > 0) {
              //              println(ElevatedTile(
              //                tileX, tileY, byte1, byte2, byte3, height + tileY))
              buffer.append(ElevatedTile(
                tileX, tileY, byte1, byte2, byte3, height + tileY))
            }
          }
        }
      }
    }

    buffer.sortBy(_.zPriority).toArray
  }

  /**
   * Vehicles can only move on specifically allowed autotiles. All other tiles
   * are disallowed.
   */
  def getVehicleBlockedDirsOf(xTile: Int, yTile: Int, vehicleId: Int): Byte = {
    import RpgMap._
    import DirectionMasks._
    val xIdx = xTile * bytesPerTile

    // Test top layer first, as if the top layer provides an answer, there is
    // no need to test subsequent layers
    for (layerAry <- List(mapData.topLayer, mapData.midLayer, mapData.botLayer)) {
      val row = layerAry(yTile)
      val byte1 = row(xIdx)
      val byte2 = row(xIdx + 1)
      val byte3 = row(xIdx + 2)

      if (byte1 < 0) {
        if (byte1 == autotileByte) {
          val tiledata = autotiles(byte2).metadata
          return tiledata.vehicleDirs(vehicleId)
        } else {
          // Empty tile: Do nothing... just continue with next layer
        }
      } else {
        return ALLCARDINAL.toByte
      }
    }

    return ALLCARDINAL.toByte
  }

  def getBlockedDirsOf(xTile: Int, yTile: Int): Byte = {
    import RpgMap._
    import DirectionMasks._
    val xIdx = xTile * bytesPerTile

    // Test top layer first, as if the top layer provides an answer, there is
    // no need to test subsequent layers
    for (layerAry <- List(mapData.topLayer, mapData.midLayer, mapData.botLayer)) {
      val row = layerAry(yTile)
      val byte1 = row(xIdx)
      val byte2 = row(xIdx + 1)
      val byte3 = row(xIdx + 2)

      if (byte1 < 0) {
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
                    dy: Float, inVehicle: Boolean): (Boolean, Int) = {
    if (!entity.collisionOn)
      return (false, 0)

    val box = entity.getBoundingBox().offsetted(dx, dy)
    val newX = x + dx
    val newY = y + dy
    val (minX, minY, maxX, maxY) = (box.minX, box.minY, box.maxX, box.maxY)

    // Need 'floor' because (-0.5).toInt == 0
    val minXTile = minX.floor.toInt
    val minYTile = minY.floor.toInt
    val maxXTile = maxX.floor.toInt
    val maxYTile = maxY.floor.toInt

    // 1. Test for going off map edge
    if (!map.metadata.withinBounds(minXTile, minYTile) ||
      !map.metadata.withinBounds(maxXTile, maxYTile))
      return (true, 0)

    var blocked = false
    // Being blocked in the positive direction adds -1.
    // Being blocked in the negative direction adds  1.
    var reroute = 0f

    def processTile(xTile: Int, yTile: Int, dir: Int,
                    rerouteSign: Int): Unit = {
      val blockedDirs =
        if (inVehicle)
          getVehicleBlockedDirsOf(xTile, yTile, entity.inVehicleId)
        else
          getBlockedDirsOf(xTile, yTile)

      if (blockedDirs == NONE)
        return

      if (flagged(blockedDirs, dir)) {
        blocked = true
        reroute += rerouteSign
        return
      }

      // Finally, check for blocking due to walls running parallel to direction
      // of movement.
      if ((dir & (EAST | WEST)) > 0) {
        val northWallBlock = flagged(blockedDirs, NORTH) &&
          box.contains(BoundingBox(xTile, yTile, xTile + 1, yTile))
        val southWallBlock = flagged(blockedDirs, SOUTH) &&
          box.contains(BoundingBox(xTile, yTile + 1, xTile + 1, yTile + 1))

        if (northWallBlock) {
          blocked = true
          reroute += newY - yTile
        }
        if (southWallBlock) {
          blocked = true
          reroute += newY - (yTile + 1)
        }
      } else {
        val eastWallBlock = flagged(blockedDirs, EAST) &&
          box.contains(BoundingBox(xTile + 1, yTile, xTile + 1, yTile + 1))
        val westWallBlock = flagged(blockedDirs, WEST) &&
          box.contains(BoundingBox(xTile, yTile, xTile, yTile + 1))

        if (eastWallBlock) {
          blocked = true
          reroute += newX - (xTile + 1)
        }
        if (westWallBlock) {
          blocked = true
          reroute += newX - xTile
        }
      }
    }

    if (math.abs(dx) > math.abs(dy)) {
      processTile(maxXTile, minYTile, WEST, 1)
      processTile(maxXTile, maxYTile, WEST, -1)
      processTile(minXTile, minYTile, EAST, 1)
      processTile(minXTile, maxYTile, EAST, -1)
    } else {
      processTile(minXTile, maxYTile, NORTH, 1)
      processTile(maxXTile, maxYTile, NORTH, -1)
      processTile(minXTile, minYTile, SOUTH, 1)
      processTile(maxXTile, minYTile, SOUTH, -1)
    }

    return (blocked, math.signum(reroute).toInt)
  }

  // Generate texture atlas, nearest neighbor with no mipmaps
  val atlasTiles =
    if (renderingOffForTesting)
      null
    else
      packerTiles.generateTextureAtlas(
        TextureFilter.Nearest, TextureFilter.Nearest, false)

  def dispose() = {
    if (atlasTiles != null)
      atlasTiles.dispose()
  }
}