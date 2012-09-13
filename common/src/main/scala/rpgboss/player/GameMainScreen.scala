package rpgboss.player

import com.badlogic.gdx.Screen
import java.io.File
import rpgboss.model._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.utils.Logger

class MutableMapLoc(var map: Int = -1, var x: Float = 0, var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)
  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

class GameMainScreen(game: MyGame) extends Screen {
  val project = game.project
  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger() 
  var map: RpgMap = RpgMap.readFromDisk(project, project.data.startingLoc.map)
  var mapData: RpgMapData = map.readMapData().get
  
  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down
  
  var autotiles: Array[Autotile] = null
  var tilesets: Array[Tileset] = null
  var spritesets: Map[String, Spriteset] = null
  
  /*
   * SpriteBatch manages its own matrices. By default, it sets its modelview
   * matrix to the identity, and the projection matrix to an orthographic
   * projection with its lower left corner of the screen at (0, 0) and its
   * upper right corner at (Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
   * 
   * This makes the eye-coordinates the same as the screen-coordinates.
   * 
   * If you'd like to specify your objects in some other space, simply
   * change the projection and modelview (transform) matrices.
   */
  var batch: SpriteBatch = new SpriteBatch()
  
  var atlasTiles: TextureAtlas = null
  var atlasSprites: TextureAtlas = null
  
  // in units of tiles
  var screenW = 20.0
  var screenH = 15.0
  
  // Where in the "second" we are. Varies from 0 to 1.0
  var accumDelta : Float = 0.0f
  
  // camera position and boundaries
  val cameraLoc = new MutableMapLoc()
  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0
  
  // protagonist location
  var characterLoc = new MutableMapLoc()
  var characterDirection : Int = Spriteset.DirectionOffsets.SOUTH
  var characterMoving = false
  
  def setCameraLoc(loc: MapLoc) = {
    cameraLoc.set(loc)
    cameraL = loc.x - screenW/2
    cameraR = loc.x + screenW/2
    cameraT = loc.y - screenH/2
    cameraB = loc.y + screenH/2
    tileCamera.position.x = loc.x
    tileCamera.position.y = loc.y
    tileCamera.update()
  }
  
  // Other creation stuff that was formerly in create()
  { 
    setCameraLoc(project.data.startingLoc)
    characterLoc.set(project.data.startingLoc)
    
    val packerTiles = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    
    autotiles = project.data.autotiles.map { name =>
      Autotile.readFromDisk(project, name)
    }
    
    // Pack all the autotiles
    autotiles.map { autotile =>
      val autotilePix = new Pixmap(
          Gdx.files.absolute(autotile.dataFile.getAbsolutePath()))
      
      packerTiles.pack("autotile/%s".format(autotile.name), autotilePix)

      autotilePix.dispose()
    }
    
    // Pack all tilesets
    tilesets = map.metadata.tilesets.map(
        name => Tileset.readFromDisk(project, name)).toArray[Tileset]
    
    tilesets.map { tileset =>
      val tilesetPix = new Pixmap(
          Gdx.files.absolute(tileset.dataFile.getAbsolutePath()))
      
      packerTiles.pack("tileset/%s".format(tileset.name), tilesetPix)
      
      tilesetPix.dispose()
    }
    
    logger.info("Packed tilesets and autotiles into %d pages".format(
        packerTiles.getPages().size))
    
    // Generate texture atlas, nearest neighbor with no mipmaps
    atlasTiles = packerTiles.generateTextureAtlas(
        TextureFilter.Nearest, TextureFilter.Nearest, false)
    
    // Generate and pack sprites
    spritesets = Map() ++ Spriteset.list(project).map(
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
    
    logger.info("Packed sprites into %d pages".format(
        packerSprites.getPages().size))
    
    atlasSprites = packerSprites.generateTextureAtlas(
        TextureFilter.Nearest, TextureFilter.Nearest, false)
    
    
  }
  
  override def dispose() {
    // Dispose of texture atlas
    atlasTiles.dispose()
  }
  override def hide() {}
  override def pause() {}
  
  override def render(delta: Float) {
    import Tileset._
    
    // Set delta time
    accumDelta = (accumDelta + delta)% 1.0f
    
    // Log fps
    fps.log()
    
    // Update game state
    {
      import com.badlogic.gdx.Input._

      def isKeyPressed(x: Int) = Gdx.input.isKeyPressed(x)
      
      characterMoving = false
      val baseSpeed = 0.05 // tiles per frame 
      
      val projSpeed =
        if((isKeyPressed(Keys.LEFT) || isKeyPressed(Keys.RIGHT)) &&
           (isKeyPressed(Keys.UP) || isKeyPressed(Keys.DOWN))) {
          (baseSpeed/math.sqrt(2.0)).toFloat
        } else {
          baseSpeed.toFloat
        }
      
      if(Gdx.input.isKeyPressed(Keys.LEFT)) {
        characterMoving = true
        characterLoc.x -= projSpeed
        characterDirection = Spriteset.DirectionOffsets.WEST
      } else if(Gdx.input.isKeyPressed(Keys.RIGHT)) {
        characterMoving = true
        characterLoc.x += projSpeed
        characterDirection = Spriteset.DirectionOffsets.EAST
      }
      
      if(Gdx.input.isKeyPressed(Keys.UP)) {
        characterMoving = true
        characterLoc.y -= projSpeed
        characterDirection = Spriteset.DirectionOffsets.NORTH
      } else if(Gdx.input.isKeyPressed(Keys.DOWN)) {
        characterMoving = true
        characterLoc.y += projSpeed
        characterDirection = Spriteset.DirectionOffsets.SOUTH
      }
    }    
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    
    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)
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
              
              val frameIdx = (accumDelta*autotile.frames).toInt
                
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
    
    val step = if(characterMoving) {
      val stepsPerSec = 8 // MUST BE EVEN
      val stepTime = 1.0f/stepsPerSec
      if((accumDelta / stepTime).toInt % 4 == 0) {
        //println("Step 1: " + deltaTime.toString())
        Spriteset.Steps.STEP1
      } else if((accumDelta / stepTime).toInt % 4 == 2) {
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
        characterDirection,
        step)
    
    val (dstOriginX, dstOriginY, dstWTiles, dstHTiles) = 
      protagonistSpriteset.dstPosition(characterLoc.x, characterLoc.y)
      
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
  
  
  override def resize(x: Int, y: Int) {}
  override def resume() {}
  override def show() {}
}
