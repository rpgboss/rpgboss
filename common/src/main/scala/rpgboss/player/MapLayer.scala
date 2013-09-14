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
    def cameraLoc = state.persistent.cameraLoc
    cameraL = cameraLoc.x - screenW / 2
    cameraR = cameraLoc.x + screenW / 2
    cameraT = cameraLoc.y - screenH / 2
    cameraB = cameraLoc.y + screenH / 2
    tileCamera.position.x = cameraLoc.x
    tileCamera.position.y = cameraLoc.y
    tileCamera.update()

    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)
  }

  // Generate and pack sprites
  val spritesets = Map() ++ Spriteset.list(project).map(
    name => (name, Spriteset.readFromDisk(project, name)))

  val packerSprites = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
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

  // Update. Called on Gdx thread before render.
  def update(delta: Float) = {
  }

  def render() = state.mapAndAssetsOption map { mapAndAssets =>
    import mapAndAssets._

    import Tileset._

    updateCameraLoc()

    batch.begin()

    // Leftmost, rightmost, topmost, bottom-most tiles to render
    val tileL = math.max(0, cameraL.toInt)
    val tileR = math.min(map.metadata.xSize - 1, cameraR.toInt + 1)
    val tileT = math.max(0, cameraT.toInt)
    val tileB = math.min(map.metadata.ySize - 1, cameraB.toInt + 1)

    // Where we are in the current second. Varies within [0, 1.0)
    val whereInSecond = (System.currentTimeMillis() % 1000).toFloat / 1000f
    /*println("Render")
    println(tileL)
    println(tileR)
    println(tileT)
    println(tileB)*/
    for (
      layerAry <- List(
        mapData.botLayer, mapData.midLayer, mapData.topLayer)
    ) {
      for (tileY <- tileT to tileB) {
        val row = layerAry(tileY)
        import RpgMap.bytesPerTile
        for (tileX <- tileL to tileR) {
          val idx = tileX * bytesPerTile
          val byte1 = row(idx)
          val byte2 = row(idx + 1)
          val byte3 = row(idx + 2)

          if (byte1 < 0) {
            if (byte1 == RpgMap.autotileByte) { // Autotile
              val autotile = autotiles(byte2)
              val region =
                atlasTiles.findRegion("autotile/%s".format(autotile.name))

              val frameIdx = (whereInSecond * autotile.frames).toInt

              val srcDestPositions = autotile.getHalfTiles(byte3, frameIdx)

              srcDestPositions map {
                case ((srcXHt, srcYHt), (dstXHt, dstYHt)) =>
                  batch.draw(
                    region.getTexture(),
                    tileX.toFloat + dstXHt * 0.5f,
                    tileY.toFloat + dstYHt * 0.5f,
                    0.5f, 0.5f,
                    region.getRegionX() + srcXHt * halftile,
                    region.getRegionY() + srcYHt * halftile,
                    halftile, halftile,
                    false, true)
              }
            }
          } else { // Regular tile
            //println("Draw regular tile")
            val region =
              atlasTiles.findRegion("tileset/%s".format(tilesets(byte1).name))
            batch.draw(
              region.getTexture(),
              tileX.toFloat,
              tileY.toFloat,
              1.0f, 1.0f,
              region.getRegionX() + byte2 * tilesize,
              region.getRegionY() + byte3 * tilesize,
              tilesize, tilesize,
              false, true)

          }
        }
      }
    }

    // Render the player event
    val entities: List[Entity] = state.playerEntity :: state.eventEntities
    entities.sortBy(_.y).foreach(_.render(batch, atlasSprites))

    batch.end()
  }

  def dispose() = {
    atlasSprites.dispose()
    batch.dispose()
  }
}