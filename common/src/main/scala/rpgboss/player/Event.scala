package rpgboss.player
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/*
 * Position is marked as such:
 * __TOP__
 * |     |
 * |_____|
 * |     |
 * |  *  |   * = position
 * |_____|
 *   BOT
 * It is the bottom center of the sprite.
 * 
 * Bottom edge length is boundBoxTiles
 */
class Event(
    game: MyGame,
    var x: Float = 0f, 
    var y: Float = 0f, 
    var dir: Int = SpriteSpec.Directions.SOUTH,
    var initialSprite: Option[SpriteSpec] = None) 
{
  var vx: Float = 0f
  var vy: Float = 0f
  var movingSince: Long = 0
  var isMoving: Boolean = false
  var previouslyMoving: Boolean = false
  var msPerStep = 128
  
  var spriteset: Spriteset = null
  var spriteIdx: Int = -1
  var graphicWTiles: Float = 0f
  var graphicHTiles: Float = 0f
  var boundBoxTiles: Float = 1.0f
  var stillStep = SpriteSpec.Steps.STILL
  
  def setMoving(moving: Boolean, vxArg: Float = 0f, vyArg: Float = 0f) = {
    isMoving = moving
    
    if(moving) {
      vx = vxArg
      vy = vyArg
      if(!previouslyMoving) {
        movingSince = System.currentTimeMillis()
        previouslyMoving = true
      }
    } else {
      previouslyMoving = false
    }
  }
  
  def currentStep() = if(isMoving) {
    import SpriteSpec.Steps._
    
    val movingDuration = System.currentTimeMillis() - movingSince
    val timeInCycle = (movingDuration % (msPerStep*TOTALSTEPS)).toInt
    
    val stepNumber = (timeInCycle / msPerStep) 
    
    (stillStep + stepNumber) % TOTALSTEPS
  } else {
    stillStep
  }
  
  def setSprite(spriteSpec: Option[SpriteSpec]) = spriteSpec map { s =>
    spriteset = game.mapLayer.spritesets(s.spriteset)
    spriteIdx = s.spriteIndex
    graphicWTiles = spriteset.tileW.toFloat/Tileset.tilesize.toFloat
    graphicHTiles = spriteset.tileH.toFloat/Tileset.tilesize.toFloat
    // Multiplier to allow sprites to fit into other tiles
    boundBoxTiles = graphicWTiles*0.8f
    dir = s.dir
    stillStep = s.step
  } getOrElse {
    spriteset = null
    boundBoxTiles = 1.0f*0.8f
  }
  
  /**
   * Given the position we want to sprite to be. Give the origin and size of
   * the sprite reasonably. If we specify the sprite with a size of 1.0 to be
   * at (2.5, 7.5), the sprite's bottom should be at 8.0
   * 
   * Note that the destination's "origin" is the top-left of the sprite, since
   * we have flipped the y-axis in libgdx.
   */
  def dstPosition(posX: Float, posY: Float) : (Float, Float, Float, Float) = {
    val dstOriginX = posX - graphicWTiles/2.0f
    val dstOriginY = posY - graphicHTiles + boundBoxTiles/2
    
    (dstOriginX, dstOriginY, graphicWTiles, graphicHTiles)
  }
  
  def update(delta: Float) = {
    // Handle moving
    if(isMoving) {
      import math._
      
      val totalDx = delta*vx
      val totalDy = delta*vy
      
      var dxTravelled = 0f
      var dyTravelled = 0f
      
      def travelBlocked(dxArg: Float, dyArg: Float) = {
        game.mapLayer.mapAndAssetsOption map { mapAndAssets =>
          mapAndAssets.mapCollisionBox(x, y, dxArg, dyArg, boundBoxTiles)
        } getOrElse false
      }
      
      var travelDone = false
      while(!travelDone)    
      {
        // Increment to travel and test collisions with
        // This is so that the increments are along the travel line set by
        // totalDx and totalDy
        val (dx, dy) = if(abs(totalDx) > abs(totalDy)) {
          val dx = min(abs(0.05f), abs(totalDx))*signum(totalDx)
          val dy = totalDy/totalDx*dx
          (dx, dy)
        } else {
          val dy = min(abs(0.05f), abs(totalDy))*signum(totalDy)
          val dx = totalDx/totalDy*dy
          (dx, dy)
        }
        
        val travellingInX = (totalDx != 0) && !travelBlocked(dx, 0)
        
        if(travellingInX) {
          x += dx
          dxTravelled += dx
          if(abs(dxTravelled) >= abs(totalDx)) {
            travelDone = true
          }
        }
        
        val travellingInY = (totalDy != 0) && !travelBlocked(0, dy)
        
        if(travellingInY) {
          y += dy
          dyTravelled += dy
          
          if(abs(dyTravelled) >= abs(totalDy)) {
            travelDone = true
          }
        }
        
        if(!travellingInX && !travellingInY) {
          travelDone = true
        }
      }
    }
  }
  
  def render(batch: SpriteBatch, atlasSprites: TextureAtlas) = {
    if(spriteset != null) {
      val region = atlasSprites.findRegion(spriteset.name)
      val step = currentStep()
      val (srcX, srcY) = spriteset.srcTexels(spriteIdx, dir, step)
      
      val (dstOriginX, dstOriginY, dstWTiles, dstHTiles) = dstPosition(x, y)
      
      val srcXInRegion = region.getRegionX() + srcX
      val srcYInRegion = region.getRegionY() + srcY
      
      // Draw protagonist
      batch.draw(
          region.getTexture(),
          dstOriginX.toFloat, 
          dstOriginY.toFloat,
          dstWTiles, dstHTiles,
          srcXInRegion, 
          srcYInRegion,
          spriteset.tileW, 
          spriteset.tileH,
          false, true)
    }
  }
}