package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.player.entity._

/**
 * *
 * This layer renders stuff on the map.
 *
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class MapScreen(val game: RpgGame)
  extends RpgScreen {
  def project = game.project
  val batch = new SpriteBatch()

  var screenW = 20.0
  var screenH = 15.0

  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0

  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down

  // current map
  var mapAndAssetsOption: Option[MapAndAssets] = None
  def mapName = mapAndAssetsOption.map(_.map.name)

  // protagonist. Modify all these things on the Gdx thread
  var playerEntity: PlayerEntity = new PlayerEntity(game, this)

  val camera = new MapCamera(game)

  // All the events on the current map, including the player event
  var eventEntities = Map[Int, EventEntity]()

  def updateMapAssets(mapNameOption: Option[String]) = {
    if (mapNameOption.isDefined) {
      val mapName = mapNameOption.get
      mapAndAssetsOption.map(_.dispose())

      val mapAndAssets = new MapAndAssets(project, mapNameOption.get)
      mapAndAssetsOption = Some(mapAndAssets)
      eventEntities = mapAndAssets.mapData.events.map {
        case (k, v) => ((k, new EventEntity(game, mapName, v)))
      }
    } else {
      mapAndAssetsOption.map(_.dispose())
      mapAndAssetsOption = None
      eventEntities = Map.empty
    }
  }

  def updateCameraLoc() = {
    cameraL = camera.x - screenW / 2
    cameraR = camera.x + screenW / 2
    cameraT = camera.y - screenH / 2
    cameraB = camera.y + screenH / 2
    tileCamera.position.x = camera.x
    tileCamera.position.y = camera.y
    tileCamera.update()

    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)
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

  override def onFirstShow() = {
  }
  
  // Update. Called on Gdx thread before render.
  def update(delta: Float) = {
    // Update events, including player event
    eventEntities.values.foreach(_.update(delta))
    playerEntity.update(delta)

    camera.update(delta)

    windowManager.update(delta)
  }

  def renderMap() = mapAndAssetsOption map { mapAndAssets =>
    import mapAndAssets._

    import Tileset._

    updateCameraLoc()

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
      (playerEntity :: eventEntities.values.toList)
        .filter(e => (e.x >= cameraL - 2) && (e.x <= cameraR + 2) &&
                     (e.y >= cameraT - 2) && (e.y <= cameraB + 2))
        .sortBy(_.y).toArray

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
    
    windowManager.preMapRender()
    renderMap()
    windowManager.render()
  }
  
  override def dispose() = {
    mapAndAssetsOption.map(_.dispose())
    batch.dispose()
    
    super.dispose()
  }
}