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
class MapLayer(game: MyGame) extends IsDownInputHandler {
  // Add input handling
  game.inputs.prepend(this)
  
  def project = game.project
  def batch = game.batch
  
  var map: RpgMap = RpgMap.readFromDisk(project, project.data.startingLoc.map)
  var mapData: RpgMapData = map.readMapData().get
  
  var screenW = 20.0
  var screenH = 15.0
  
  // camera position and boundaries
  val cameraLoc = new MutableMapLoc()
  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0
  
  val tileCamera: OrthographicCamera = new OrthographicCamera()
  tileCamera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down
  
  // protagonist location
  val characterLoc = new MutableMapLoc()
  characterLoc.set(project.data.startingLoc)
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
  setCameraLoc(project.data.startingLoc)
  
  /***
   * This section is all the stuff that finds the graphics and packs it into
   * texture atlases.
   */
  
  val packerTiles = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    
  val autotiles = project.data.autotiles.map { name =>
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
  
  game.logger.info("Packed tilesets and autotiles into %d pages".format(
      packerTiles.getPages().size))
  
  // Generate texture atlas, nearest neighbor with no mipmaps
  val atlasTiles = packerTiles.generateTextureAtlas(
      TextureFilter.Nearest, TextureFilter.Nearest, false)
    
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
  
  // Update
  def update() = {
    import MyKeys._
    
    characterMoving = false
    val baseSpeed = 0.05 // tiles per frame 
    
    val projSpeed =
      if((down(Left) || down(Right)) &&
         (down(Up) || down(Down))) {
        (baseSpeed/math.sqrt(2.0)).toFloat
      } else {
        baseSpeed.toFloat
      }
    
    if(down(Left)) {
      characterMoving = true
      characterLoc.x -= projSpeed
      characterDirection = Spriteset.DirectionOffsets.WEST
    } else if(down(Right)) {
      characterMoving = true
      characterLoc.x += projSpeed
      characterDirection = Spriteset.DirectionOffsets.EAST
    }
    
    if(down(Up)) {
      characterMoving = true
      characterLoc.y -= projSpeed
      characterDirection = Spriteset.DirectionOffsets.NORTH
    } else if(down(Down)) {
      characterMoving = true
      characterLoc.y += projSpeed
      characterDirection = Spriteset.DirectionOffsets.SOUTH
    }
  }    
      
  def render() = {
    import Tileset._
    
    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(tileCamera.combined)
    
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
    
    val step = if(characterMoving) {
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
        characterDirection,
        step)
    
    val (dstOriginX, dstOriginY, dstWTiles, dstHTiles) = 
      protagonistSpriteset.dstPosition(characterLoc.x, characterLoc.y)
    
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
  }
      
  def dispose() = {
    atlasTiles.dispose()
    atlasSprites.dispose()
    game.inputs.remove(this)
  }
}