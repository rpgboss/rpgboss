package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import rpgboss.model.SpriteSpec.Steps.TOTALSTEPS
import rpgboss.player.MyGame
import scala.math.abs
import scala.math.min
import scala.math.signum
import rpgboss.model.event.EventHeight

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
abstract class Entity(
  game: MyGame,
  var x: Float = 0f,
  var y: Float = 0f,
  var dir: Int = SpriteSpec.Directions.SOUTH,
  var initialSprite: Option[SpriteSpec] = None) {

  private var vx: Float = 0f
  private var vy: Float = 0f
  var movingSince: Long = 0
  var isMoving: Boolean = false
  var previouslyMoving: Boolean = false
  var msPerStep = 128

  var spriteset: Spriteset = null
  var spriteIdx: Int = -1
  var graphicWTiles: Float = 0f
  var graphicHTiles: Float = 0f

  var stillStep = SpriteSpec.Steps.STILL
  var boundingBoxHalfsize = 0.5f

  def setBoundingBoxHalfsize(halfsizeArg: Float) = {
    // Normalize to prevent bounding boxes too large to be used on tile map
    val halfsize = math.min((2.0f - collisionDeltas) / 2.0f, halfsizeArg)

    boundingBoxHalfsize = halfsize
  }

  def getBoundingBox() = {
    BoundingBox(x - boundingBoxHalfsize, y - boundingBoxHalfsize,
      x + boundingBoxHalfsize, y + boundingBoxHalfsize)
  }

  def getMapCollisions(dxArg: Float, dyArg: Float) = {
    game.state.mapAndAssetsOption map { mapAndAssets =>
      mapAndAssets.getCollisions(this, x, y, dxArg, dyArg)
    } getOrElse (true, 0)
  }

  setBoundingBoxHalfsize(0.5f)

  def collisionDeltas = 0.05f

  def setMoving(moving: Boolean, vxArg: Float = 0f, vyArg: Float = 0f) = {
    isMoving = moving

    if (moving) {
      vx = vxArg
      vy = vyArg
      if (!previouslyMoving) {
        movingSince = System.currentTimeMillis()
        previouslyMoving = true
      }
    } else {
      previouslyMoving = false
    }
  }

  def currentStep() = if (isMoving) {
    import SpriteSpec.Steps._

    val movingDuration = System.currentTimeMillis() - movingSince
    val timeInCycle = (movingDuration % (msPerStep * TOTALSTEPS)).toInt

    val stepNumber = (timeInCycle / msPerStep) + 1 // start on a step

    (stillStep + stepNumber) % TOTALSTEPS
  } else {
    stillStep
  }

  def setSprite(spriteSpec: Option[SpriteSpec]) = spriteSpec map { s =>
    spriteset = game.mapLayer.spritesets(s.spriteset)
    spriteIdx = s.spriteIndex
    graphicWTiles = spriteset.tileW.toFloat / Tileset.tilesize.toFloat
    graphicHTiles = spriteset.tileH.toFloat / Tileset.tilesize.toFloat
    // Minus the delta to allow events to fit into tiles easily
    setBoundingBoxHalfsize((graphicWTiles - collisionDeltas) * 0.5f)
    dir = s.dir
    stillStep = s.step
  } getOrElse {
    spriteset = null
    setBoundingBoxHalfsize((1.0f - collisionDeltas) * 0.5f)
  }

  /**
   * Finds all events with which this dxArg and dyArg touches
   */
  def getAllEventTouches(dxArg: Float, dyArg: Float) = {
    val boundingBox = getBoundingBox()
    game.state.npcEvts.filter(npc => {
      val npcBoundingBox = npc.getBoundingBox()
      boundingBox.contains(npcBoundingBox)
    })
  }

  /**
   * This method is called when event collides against another event during
   * movement.
   */
  def eventTouchCallback(touchedNpcs: List[NonplayerEntity])

  def update(delta: Float) = {
    // Handle moving
    if (isMoving) {
      import math._

      var totalDx = delta * vx
      var totalDy = delta * vy

      var dxTravelled = 0f
      var dyTravelled = 0f

      var travelDone = false
      while (!travelDone) {
        // Increment to travel and test collisions with
        // This is so that the increments are along the travel line set by
        // totalDx and totalDy
        val (dx, dy) = if (abs(totalDx) > abs(totalDy)) {
          val dx = min(abs(collisionDeltas), abs(totalDx)) * signum(totalDx)
          val dy = totalDy / totalDx * dx
          (dx, dy)
        } else {
          val dy = min(abs(collisionDeltas), abs(totalDy)) * signum(totalDy)
          val dx = totalDx / totalDy * dy
          (dx, dy)
        }

        var dxThisLoop = 0f
        var dyThisLoop = 0f

        var isSliding = false

        val evtsTouched = getAllEventTouches(dx, dy)
        eventTouchCallback(evtsTouched)
        val evtBlocking =
          evtsTouched.exists(_.evtState.height == EventHeight.SAME.id)

        // Move along x
        if (!evtBlocking && totalDx != 0) {
          // Determine collisions in x direction on the y-positive corner
          // and the y negative corner of the bounding box
          val (mapBlocked, mapReroute) = getMapCollisions(dx, 0)

          // Conventional movement if it succeeeds
          if (!mapBlocked) {
            dxThisLoop += dx
            x += dx
            dxTravelled += dx
          } else if (totalDy == 0) {
            // Conventional movement blocked. Try sliding perpendicularly
            if (mapReroute != 0) {
              totalDy += mapReroute * abs(totalDx)
              isSliding = true
            }
          }
        }

        // Move along y
        if (!evtBlocking && totalDy != 0) {
          // Determine collisions in x direction on the y-positive corner
          // and the y negative corner of the bounding box
          val (mapBlocked, mapReroute) = getMapCollisions(0, dy)

          // Conventional movement if it succeeeds
          if (!mapBlocked) {
            dyThisLoop += dy
            y += dy
            dyTravelled += dy
          } else if (totalDx == 0) {
            // Conventional movement blocked. Try sliding perpendicularly
            if (mapReroute != 0) {
              totalDx += mapReroute * abs(totalDy)
              isSliding = true
            }
          }
        }

        // Was able to move conventionally
        if (dxThisLoop != 0 || dyThisLoop != 0) {
          // Check if we are done travelling by distance measure
          if ((totalDx != 0 && abs(dxTravelled) >= abs(totalDx)) ||
            (totalDy != 0 && abs(dyTravelled) >= abs(totalDy))) {
            travelDone = true
          }
        } else {
          // Man, we can't even slide. Let's quit
          if (!isSliding) {
            travelDone = true
            isMoving = false
          }
        }
      }
    }
  }

  def render(batch: SpriteBatch, atlasSprites: TextureAtlas) = {
    if (spriteset != null) {
      val region = atlasSprites.findRegion(spriteset.name)
      val step = currentStep()
      val (srcX, srcY) = spriteset.srcTexels(spriteIdx, dir, step)

      /*
       * Given the definition of the position (see beginning of the file),
       * calculate the top-left corner of the graphic we draw.
       * We use top-left because we have flipped the y-axis in libgdx to match
       * the map coordinates we use.
       */
      val dstOriginX: Float = x - graphicWTiles / 2.0f
      val dstOriginY: Float = y - graphicHTiles + graphicWTiles / 2

      val srcXInRegion = region.getRegionX() + srcX
      val srcYInRegion = region.getRegionY() + srcY

      // Draw protagonist
      batch.draw(
        region.getTexture(),
        dstOriginX.toFloat,
        dstOriginY.toFloat,
        graphicWTiles, graphicHTiles,
        srcXInRegion,
        srcYInRegion,
        spriteset.tileW,
        spriteset.tileH,
        false, true)
    }
  }
}

case class BoundingBox(minX: Float, minY: Float, maxX: Float, maxY: Float) {
  def contains(o: BoundingBox) =
    o.maxX >= minX && o.minX <= maxX && o.maxY >= minY && o.minY <= maxY

  def offseted(dx: Float, dy: Float) =
    copy(minX + dx, minY + dy, maxX + dx, maxY + dy)
}