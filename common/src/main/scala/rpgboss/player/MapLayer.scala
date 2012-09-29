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

/***
 * This layer renders stuff on the map.
 * 
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class MapLayer(game: MyGame) extends MoveInputHandler {
  // Add input handling
  game.inputs.prepend(this)
  
  def project = game.project
  def state = game.state
  val batch = new SpriteBatch()
  
  var mapIdOnLastUpdate = -1
  var mapAndAssetsOption : Option[MapAndAssets] = None
  
  var screenW = 20.0
  var screenH = 15.0
  
  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0
  
  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down
  
  def updateCameraLoc() = {
    cameraL = state.cameraLoc.x - screenW/2
    cameraR = state.cameraLoc.x + screenW/2
    cameraT = state.cameraLoc.y - screenH/2
    cameraB = state.cameraLoc.y + screenH/2
    tileCamera.position.x = state.cameraLoc.x
    tileCamera.position.y = state.cameraLoc.y
    tileCamera.update()
  }
  
  // Set the projection matrix to the combined camera matrices
  // This seems to be the only thing that works...
  batch.setProjectionMatrix(tileCamera.combined)
    
  // Generate and pack sprites
  val spritesets = Map() ++ Spriteset.list(project).map(
      name => (name, Spriteset.readFromDisk(project, name)))
  
  val packerSprites = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
  spritesets.foreach { 
    case (name, spriteset) =>
      val srcPixmap = new Pixmap(
          Gdx.files.absolute(spriteset.dataFile.getAbsolutePath()))
      
      val srcFormat = srcPixmap.getFormat()
      if(srcFormat == Pixmap.Format.RGBA8888 || 
          srcFormat == Pixmap.Format.RGBA4444) {
          
        // Already has transparency. Pack and dispose.
        packerSprites.pack(spriteset.name, srcPixmap)
        srcPixmap.dispose()
      } else if(srcFormat == Pixmap.Format.RGB888){
        // TODO: Optimize pixel transfer         
        
        // Build transparency from (0, 0) pixel
        val dstPixmap = new Pixmap(
          srcPixmap.getWidth(), srcPixmap.getHeight(), Pixmap.Format.RGBA8888)
  
        val transparentVal = srcPixmap.getPixel(0, 0)
        
        for(y <- 0 until srcPixmap.getHeight()) {
          for(x <- 0 until srcPixmap.getWidth()) {
            val curPixel = srcPixmap.getPixel(x, y)
            
            if(curPixel != transparentVal) {
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
  def update() = {
    if(mapIdOnLastUpdate != state.cameraLoc.map) {
      // Update internal resources for the map
      if(state.cameraLoc.map == -1) {
        mapAndAssetsOption.map(_.dispose())
        mapAndAssetsOption = None
      } else {
        mapAndAssetsOption.map(_.dispose())
        mapAndAssetsOption = 
          Some(new MapAndAssets(project, state.cameraLoc.map))
      }
      mapIdOnLastUpdate = state.cameraLoc.map
    }
    
    import MyKeys._
        
    state.playerMoving = false
    
    val baseSpeed = 0.05 // tiles per frame
    
    val projSpeed =
      if((isActive(Left) || isActive(Right)) &&
         (isActive(Up) || isActive(Down))) {
        (baseSpeed/math.sqrt(2.0)).toFloat
      } else {
        baseSpeed.toFloat
      }
    
    if(isActive(Left)) {
      state.playerMoving = true
      state.playerLoc.x -= projSpeed
      state.playerDir = Spriteset.DirectionOffsets.WEST
    } else if(isActive(Right)) {
      state.playerMoving = true
      state.playerLoc.x += projSpeed
      state.playerDir = Spriteset.DirectionOffsets.EAST
    }
    
    if(isActive(Up)) {
      state.playerMoving = true
      state.playerLoc.y -= projSpeed
      state.playerDir = Spriteset.DirectionOffsets.NORTH
    } else if(isActive(Down)) {
      state.playerMoving = true
      state.playerLoc.y += projSpeed
      state.playerDir = Spriteset.DirectionOffsets.SOUTH
    }
  }    
      
  def render() = mapAndAssetsOption map { mapAndAssets =>
    import mapAndAssets._
    
    import Tileset._
    
    batch.begin()
    
    // Leftmost, rightmost, topmost, bottom-most tiles to render
    val tileL = math.max(0, cameraL.toInt)
    val tileR = math.min(map.metadata.xSize-1, cameraR.toInt+1)
    val tileT = math.max(0, cameraT.toInt)
    val tileB = math.min(map.metadata.ySize-1, cameraB.toInt+1)
    /*println("Render")
    println(tileL)
    println(tileR)
    println(tileT)
    println(tileB)*/
    for(layerAry <- List(
        mapData.botLayer, mapData.midLayer, mapData.topLayer)) {
      for(tileY <- tileT to tileB) {
        val row = layerAry(tileY)
        import RpgMap.bytesPerTile
        for(tileX <- tileL to tileR) {
          val idx = tileX*bytesPerTile
          val byte1 = row(idx)
          val byte2 = row(idx+1)
          val byte3 = row(idx+2)
          
          if(byte1 < 0) {
            if(byte1 == RpgMap.autotileByte) { // Autotile
              val autotile = autotiles(byte2)
              val region = 
                atlasTiles.findRegion("autotile/%s".format(autotile.name))
              
              val frameIdx = (game.accumDelta*autotile.frames).toInt
                
              val srcDestPositions = autotile.getHalfTiles(byte3, frameIdx)
              
              srcDestPositions map {
                case ((srcXHt, srcYHt), (dstXHt, dstYHt)) =>
                  batch.draw(
                      region.getTexture(),
                      tileX.toFloat+dstXHt*0.5f,
                      tileY.toFloat+dstYHt*0.5f,
                      0.5f, 0.5f,
                      region.getRegionX() + srcXHt*halftile,
                      region.getRegionY() + srcYHt*halftile,
                      halftile, halftile,
                      false, true
                      )
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
                region.getRegionX() + byte2*tilesize, 
                region.getRegionY() + byte3*tilesize,
                tilesize, tilesize,
                false, true)
            
          }
        }
      }
    }
    
    // Draw protagonist
    val protagonistActor = project.data.actors(project.data.startingParty(0))
    
    val region =
      atlasSprites.findRegion(protagonistActor.sprite.spriteset)
    val protagonistSpriteset = spritesets(protagonistActor.sprite.spriteset)
    
    val step = if(state.playerMoving) {
      val stepsPerSec = 8 // MUST BE EVEN
      val stepTime = 1.0f/stepsPerSec
      if((game.accumDelta / stepTime).toInt % 4 == 0) {
        //println("Step 1: " + deltaTime.toString())
        Spriteset.Steps.STEP1
      } else if((game.accumDelta / stepTime).toInt % 4 == 2) {
        //println("Step 2: " + deltaTime.toString())
        Spriteset.Steps.STEP2
      } else {
        Spriteset.Steps.STILL
      }
    } else {
      Spriteset.Steps.STILL
    }
    
    val (srcX, srcY) = protagonistSpriteset.srcTexels(
        protagonistActor.sprite.spriteindex,
        state.playerDir,
        step)
    
    val (dstOriginX, dstOriginY, dstWTiles, dstHTiles) = 
      protagonistSpriteset.dstPosition(
          state.playerLoc.x, state.playerLoc.y)
    
    // Draw protagonist
    batch.draw(
        region.getTexture(),
        dstOriginX.toFloat, 
        dstOriginY.toFloat,
        dstWTiles, dstHTiles,
        region.getRegionX() + srcX, 
        region.getRegionY() + srcY,
        protagonistSpriteset.tileW, 
        protagonistSpriteset.tileH,
        false, true)
    
    batch.end()
  }
      
  def dispose() = {
    mapAndAssetsOption.map(_.dispose())
    atlasSprites.dispose()
    game.inputs.remove(this)
    batch.dispose()
  }
}