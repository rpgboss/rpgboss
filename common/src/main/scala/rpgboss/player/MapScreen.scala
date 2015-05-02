package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.math.Vector2
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.model.Transitions
import rpgboss.lib.Utils

/**
 * *
 * This layer renders stuff on the map.
 *
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class MapScreen(val game: RpgGame)
  extends RpgScreenWithGame
  with HasScriptConstants {

  val scriptHooks = new ScriptHookManager(scriptInterface)

  var screenWTiles: Float = screenW / Tileset.tilesize
  var screenHTiles: Float = screenH / Tileset.tilesize

  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenWTiles, screenHTiles) // y points down

  // current map
  var mapAndAssetsOption: Option[MapAndAssets] = None
  def mapName = mapAndAssetsOption.map(_.map.name)

  def playerEntity =
    allEntities(EntitySpec.playerEntityId).asInstanceOf[PlayerEntity]
  def getPlayerEntityInfo() = EntityInfo(playerEntity, this)

  def movePlayer(dx: Float, dy: Float, affixDirection: Boolean) = {
    assertOnBoundThread()
    playerEntity.moveEntity(new Vector2(dx, dy), affixDirection)
  }

  def moveEvent(id: Int, dx: Float, dy: Float, affixDirection: Boolean) = {
    assertOnBoundThread()
    val entityOpt = allEntities.get(id)
    entityOpt.map { entity =>
      entity.moveEntity(new Vector2(dx, dy), affixDirection)
    }.orNull
  }

  def getEventX(id: Int):Float = {
    val entityOpt = allEntities.get(id)
    entityOpt.map { entity =>
      return entity.x
    }.orNull
    return 0
  }

  def getEventY(id: Int):Float = {
    val entityOpt = allEntities.get(id)
    entityOpt.map { entity =>
      return entity.y
    }.orNull
    return 0
  }

  def setCameraFollow(entityId: Option[Int]) =
    cameraFollowedEntity = entityId.flatMap(allEntities.get)

  val camera = new MapCamera

  // All the events on the current map, including the player event.
  // The player event is stored with the '-1' key.
  val allEntities = collection.mutable.Map[Int, Entity]()
  var cameraFollowedEntity: Option[Entity] = None

  def setPlayerLoc(loc: MapLoc): Unit = {
    mapAndAssetsOption.map(_.dispose())
    mapAndAssetsOption = None

    allEntities.map(_._2.dispose())
    allEntities.clear()

    if (loc.map.isEmpty())
      return

    val mapName = loc.map

    val mapAndAssets =
      new MapAndAssets(project, loc.map, renderingOffForTesting)
    mapAndAssets.setLastBattlePosition(loc.x, loc.y)

    mapAndAssetsOption = Some(mapAndAssets)

    windowManager.reset()

    val distinctChars =
      project.data.enums.distinctChars ++ mapAndAssets.mapData.distinctChars
    windowManager.updateBitmapFont(distinctChars.mkString)

    allEntities.update(EntitySpec.playerEntityId, new PlayerEntity(game, this))
    playerEntity.x = loc.x
    playerEntity.y = loc.y
    playerEntity.mapName = Some(loc.map)

    playerEntity.updateSprite()

    allEntities ++= mapAndAssets.mapData.events.map {
      case (k, v) => ((k, new EventEntity(
          game.project,
          game.persistent,
          scriptInterface,
          scriptFactory,
          game.spritesets,
          mapAndAssetsOption,
          allEntities,
          mapName,
          v)))
    }

    cameraFollowedEntity = Some(playerEntity)

    playMusic(0, mapAndAssets.map.metadata.music, true,
        Transitions.fadeLength)

    updateCameraLoc(0.0f, mapAndAssets, forceSnapToEntity = true)

    var interiorValue = 0
    if(mapAndAssets.map.metadata.interior) {
      interiorValue = 1
    }

    scriptInterface.setInt("interior",interiorValue)
  }

  def persistPlayerLocation() = {
    val p = playerEntity
    assert(p.mapName.isDefined)
    game.persistent.setLoc(PLAYER_LOC, MapLoc(p.mapName.get, p.x, p.y))
  }

  def updateCameraLoc(delta: Float, mapAndAssets: MapAndAssets,
                      forceSnapToEntity: Boolean) = {
    val map = mapAndAssets.map

    camera.update(delta, cameraFollowedEntity, forceSnapToEntity, map.metadata,
                  screenWTiles, screenHTiles)

    tileCamera.position.x =
      camera.x + shakeManager.xDisplacement / Tileset.tilesize
    tileCamera.position.y =
      camera.y + shakeManager.yDisplacement / Tileset.tilesize
    tileCamera.update()
  }

  def drawTile(batch: SpriteBatch, mapAndAssets: MapAndAssets,
               whereInSecond: Float, tileX: Int, tileY: Int,
               byte1: Byte, byte2: Byte, byte3: Byte) = {
    if (byte1 < 0) {
      if (byte1 == RpgMap.autotileByte) { // Autotile
        val autotile = mapAndAssets.autotiles(byte2)
        val region = mapAndAssets.atlasTiles.findRegion(
          "autotile/%s".format(autotile.name))

        val frameIdx = (whereInSecond * autotile.frames).toInt

        val srcDestPositions = autotile.getHalfTiles(byte3, frameIdx)

        srcDestPositions map {
          case ((srcXHt, srcYHt), (dstXHt, dstYHt)) =>
            batch.draw(
              region.getTexture(),
              tileX.toFloat + dstXHt * 0.5f,
              tileY.toFloat + dstYHt * 0.5f,
              0.5f, 0.5f,
              region.getRegionX() + srcXHt * Tileset.halftile,
              region.getRegionY() + srcYHt * Tileset.halftile,
              Tileset.halftile, Tileset.halftile,
              false, true)
        }
      }
    } else { // Regular tile
      //println("Draw regular tile")
      val tileset = mapAndAssets.tilesets(byte1)
      val region =
        mapAndAssets.atlasTiles.findRegion("tileset/%s".format(tileset.name))
      batch.draw(
        region.getTexture(),
        tileX.toFloat,
        tileY.toFloat,
        1.0f, 1.0f,
        region.getRegionX() + byte2 * Tileset.tilesize,
        region.getRegionY() + byte3 * Tileset.tilesize,
        Tileset.tilesize, Tileset.tilesize,
        false, true)
    }
  }

  // Update. Called on Gdx thread before render.
  def update(delta: Float): Unit = {
    // We want the camera and window manager to update, but not anything else.
    if (windowManager.inTransition)
      return

    scriptHooks.update(delta)

    val playerOldX = playerEntity.x
    val playerOldY = playerEntity.y

    // Update events, including player event
    val eventsEnabled = game.persistent.getInt(EVENTS_ENABLED) != 0
    allEntities.values.foreach(_.update(delta, eventsEnabled))

    val playerMoveDistance =
      math.abs(playerEntity.x - playerOldX) +
      math.abs(playerEntity.y - playerOldY)

    mapAndAssetsOption map { mapAndAssets =>
      updateCameraLoc(delta, mapAndAssets, forceSnapToEntity = false)

      val minimumDistanceFromLastBattle = 3

      val encounterSettings = mapAndAssets.randomEncounterSettings
      val mapMetadata = mapAndAssets.map.metadata

      val distFromLastBattle =
        mapAndAssets.manhattanDistanceFromLastBattle(
            playerEntity.x, playerEntity.y)
      if (!encounterSettings.encounters.isEmpty &&
          distFromLastBattle > minimumDistanceFromLastBattle) {
        val chanceBattle = playerMoveDistance / encounterSettings.stepsAverage

        if (math.random < chanceBattle) {
          mapAndAssets.setLastBattlePosition(playerEntity.x, playerEntity.y)

          val encounterId = Utils.randomChoose(
              encounterSettings.encounters.map(_.encounterId),
              encounterSettings.encounters.map(_.weight.floatValue))
          game.startBattle(encounterId)
        }
      }
    }
  }

  def renderMap() = mapAndAssetsOption map { mapAndAssets =>
    import mapAndAssets._

    import Tileset._

    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)

    val cameraL = camera.x - screenWTiles / 2
    val cameraR = camera.x + screenWTiles / 2
    val cameraT = camera.y - screenHTiles / 2
    val cameraB = camera.y + screenHTiles / 2

    // Leftmost, rightmost, topmost, bottom-most tiles to render
    val tileL = math.max(0, cameraL.toInt)
    val tileR = math.min(map.metadata.xSize - 1, cameraR.toInt + 1)
    val tileT = math.max(0, cameraT.toInt)
    val tileB = math.min(map.metadata.ySize - 1, cameraB.toInt + 1)

    // Where we are in the current second. Varies within [0, 1.0)
    val whereInSecond = (System.currentTimeMillis() % 1000).toFloat / 1000f

    batch.begin()

    // Draw all the tiles
    for (layerAry <-
         List(mapData.botLayer, mapData.midLayer, mapData.topLayer)) {
      for (tileY <- tileT to tileB) {
        val row = layerAry(tileY)
        import RpgMap.bytesPerTile
        for (tileX <- tileL to tileR) {
          val idx = tileX * bytesPerTile
          val byte1 = row(idx)
          val byte2 = row(idx + 1)
          val byte3 = row(idx + 2)
          drawTile(batch, mapAndAssets, whereInSecond, tileX, tileY, byte1,
                   byte2, byte3)
        }
      }
    }

    // Get a list of all the entities within the camera's view, sorted by
    // their y position.
    val zSortedEntities =
      allEntities.values.toList
        .filter(e => (e.x >= cameraL - 2) && (e.x <= cameraR + 2) &&
                     (e.y >= cameraT - 2) && (e.y <= cameraB + 2))
        .sortBy(_.zPriority).toArray

    // Draw sprites and elevated tiles in order of z priority.
    {
      var entityI = 0
      var tileI = 0
      def tiles = mapAndAssets.elevatedTiles

      // Iterate through both the list of elevated tiles and entities,
      // drawing the 'lower' item on each iteration.
      while (entityI < zSortedEntities.size || tileI < tiles.size) {
        if (tileI == tiles.size ||
            (entityI < zSortedEntities.size &&
            zSortedEntities(entityI).y < tiles(tileI).zPriority)) {
          zSortedEntities(entityI).render(batch, game.atlasSprites)
          entityI += 1
        } else {
          val tile = tiles(tileI)

          if ((tile.tileX >= cameraL - 2) && (tile.tileX <= cameraR + 2) &&
              (tile.tileY >= cameraT - 2) && (tile.tileY <= cameraB + 2)) {
            drawTile(batch, mapAndAssets, whereInSecond, tile.tileX, tile.tileY,
                     tile.byte1, tile.byte2, tile.byte3)
          }

          tileI += 1
        }
      }
    }

    batch.end()
  }

  def render() = {
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL20.GL_BLEND)

    windowManager.preMapRender(batch, screenCamera)
    renderMap()

    windowManager.render(batch, shapeRenderer, screenCamera)
  }

  override def dispose() = {
    mapAndAssetsOption.map(_.dispose())
    super.dispose()
  }

  override def reset() = {
    super.reset()
    setPlayerLoc(MapLoc())
  }
}