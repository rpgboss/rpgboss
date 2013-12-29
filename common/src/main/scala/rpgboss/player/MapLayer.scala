package rpgboss.player

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.Texture.TextureFilter

/**
 * *
 * This layer renders stuff on the map.
 *
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class MapLayer(game: MyGame) {

  def project = game.project
  def state = game.state
  val batch = new SpriteBatch()

  var screenW = 20.0
  var screenH = 15.0

  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0

  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down

  def updateCameraLoc() = {
    cameraL = state.camera.x - screenW / 2
    cameraR = state.camera.x + screenW / 2
    cameraT = state.camera.y - screenH / 2
    cameraB = state.camera.y + screenH / 2
    tileCamera.position.x = state.camera.x
    tileCamera.position.y = state.camera.y
    tileCamera.update()

    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)
  }

  // Generate and pack sprites
  val spritesets = Map() ++ Spriteset.list(project).map(
    name => (name, Spriteset.readFromDisk(project, name)))

  val packerSprites =
    new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
  spritesets.foreach {
    case (name, spriteset) =>
      val srcPixmap = new Pixmap(
        Gdx.files.absolute(spriteset.dataFile.getAbsolutePath()))

      val srcFormat = srcPixmap.getFormat()
      if (srcFormat == Pixmap.Format.RGBA8888 ||
        srcFormat == Pixmap.Format.RGBA4444) {

        // Already has transparency. Pack and dispose.
        packerSprites.pack(spriteset.name, srcPixmap)
        srcPixmap.dispose()
      } else if (srcFormat == Pixmap.Format.RGB888) {
        // TODO: Optimize pixel transfer

        // Build transparency from (0, 0) pixel
        val dstPixmap = new Pixmap(
          srcPixmap.getWidth(), srcPixmap.getHeight(), Pixmap.Format.RGBA8888)

        val transparentVal = srcPixmap.getPixel(0, 0)

        for (y <- 0 until srcPixmap.getHeight()) {
          for (x <- 0 until srcPixmap.getWidth()) {
            val curPixel = srcPixmap.getPixel(x, y)

            if (curPixel != transparentVal) {
              dstPixmap.drawPixel(x, y, curPixel)
            }
          }
        }

        packerSprites.pack(spriteset.name, dstPixmap)
        srcPixmap.dispose()
        dstPixmap.dispose()
      }
  }

  game.logger.info("Packed sprites into %d pages".format(
    packerSprites.getPages().size))

  val atlasSprites = packerSprites.generateTextureAtlas(
    TextureFilter.Nearest, TextureFilter.Nearest, false)

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
  def update(delta: Float) = {
  }

  def render() = state.mapAndAssetsOption map { mapAndAssets =>
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
      (state.playerEntity :: state.eventEntities.values.toList)
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
          zSortedEntities(entityI).render(batch, atlasSprites)
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

  def dispose() = {
    atlasSprites.dispose()
    batch.dispose()
  }
}