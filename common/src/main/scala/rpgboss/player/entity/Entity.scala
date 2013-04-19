package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.collision.BoundingBox
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

  val boundingBox = new BoundingBox()

  def setBoundingBoxHalfsize(halfsize: Float) = {
    boundingBoxHalfsize = halfsize
    val minVector = new Vector3(x - halfsize, y - halfsize, 0)
    val maxVector = new Vector3(x + halfsize, y + halfsize, 0)
    boundingBox.set(minVector, maxVector)
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
   * Tests if there is a collision of a box moving from (x, y) to
   * (x+dx, x+dy).
   *
   * Important restriction - only one of dx or dy may be nonzero.
   * Implement diagonal movement as small alternating movements in dx and dy.
   *
   * Will make optimizations to only test for points along the leading
   *
   * @return  Returning a tuple allows us to implement sliding around corners.
   *          Essentially, if dx != 0, then tuple represents collisions in
   *          the tests of:
   *          ((xEdge+dx, y+boundingBox/2), (xEdge+dx, y-boundingBox/2))
   */
  def collisionBox(
    dx: Float,
    dy: Float,
    size: Float,
    collisionPointF: (Float, Float, Float, Float) => Boolean): (Boolean, Boolean) =
    {
      import math._

      if (dy == 0f) {
        val edgeX = x + signum(dx) * boundingBoxHalfsize
        (
          collisionPointF(edgeX, y + boundingBoxHalfsize, dx, dy),
          collisionPointF(edgeX, y - boundingBoxHalfsize, dx, dy))
      } else if (dx == 0f) {
        val edgeY = y + signum(dy) * boundingBoxHalfsize
        (
          collisionPointF(x + boundingBoxHalfsize, edgeY, dx, dy),
          collisionPointF(x - boundingBoxHalfsize, edgeY, dx, dy))
      } else {
        (true, true) // Not sure why trying to move diagonal. Disallow this.
      }
    }

  def pointInSelf(xArg: Float, yArg: Float) = {
    (
      xArg < x + boundingBoxHalfsize && xArg > x - boundingBoxHalfsize &&
      yArg < y + boundingBoxHalfsize && yArg > y - boundingBoxHalfsize)
  }

  def collisionTestPoint(x0: Float, y0: Float, dx: Float, dy: Float) = {
    pointInSelf(x0 + dx, y0 + dy)
  }

  /**
   * Finds all events with which this dxArg and dyArg touches
   */
  def getAllEventTouches(dxArg: Float, dyArg: Float) = {

    game.state.npcEvts.filter(otherEvt =>
      collisionBox(
        dxArg,
        dyArg,
        boundingBoxHalfsize,
        otherEvt.collisionTestPoint _) != (false, false))
  }

  /**
   * This method is called when event collides against another event during
   * movement.
   */
  def eventTouchCallback(touchedNpcs: List[NonplayerEntity])

  /**
   * @return Returns 3 items: (posBlocked, negBlocked)
   */
  def getMapCollisions(dxArg: Float, dyArg: Float) = {
    if (dxArg == 0 && dyArg == 0) {
      (false, false)
    } else {
      val mapCollision = game.state.mapAndAssetsOption map { mapAndAssets =>
        collisionBox(
          dxArg,
          dyArg,
          boundingBoxHalfsize,
          mapAndAssets.mapCollisionPoint _)
      } getOrElse (true, true)

      mapCollision
    }
  }

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

        var slidSuccessfully = false

        // Try conventional movement along x first
        if (totalDx != 0) {
          // Determine collisions in x direction on the y-positive corner
          // and the y negative corner of the bounding box
          val (xBlockedYPos, xBlockedYNeg) = getMapCollisions(dx, 0)

          val evtsTouched = getAllEventTouches(dx, 0)
          eventTouchCallback(evtsTouched)
          val evtBlocking =
            evtsTouched.exists(_.evtState.height == EventHeight.SAME.id)

          // Conventional movement if it succeeeds
          if (!evtBlocking && !xBlockedYPos && !xBlockedYNeg) {
            dxThisLoop += dx
            x += dx
            dxTravelled += dx
          } else if (totalDy == 0 && !evtBlocking) {
            // Conventional movement blocked. Try sliding perpendicularly
            if (!xBlockedYPos) {
              totalDy += abs(totalDx)
              slidSuccessfully = true
            } else if (!xBlockedYNeg) {
              totalDy -= abs(totalDx)
              slidSuccessfully = true
            }
          }
        }

        // Then try conventional movement along y
        if (totalDy != 0) {
          val (yBlockedXPos, yBlockedXNeg) = getMapCollisions(0, dy)

          val evtsTouched = getAllEventTouches(0, dy)
          eventTouchCallback(evtsTouched)
          val evtBlocking =
            evtsTouched.exists(_.evtState.height == EventHeight.SAME.id)

          if (!evtBlocking && !yBlockedXPos && !yBlockedXNeg) {
            dyThisLoop += dy
            y += dy
            dyTravelled += dy
          } else if (totalDx == 0 && !slidSuccessfully && !evtBlocking) {
            // Conventional movement blocked. Try sliding perpendicularly
            if (!yBlockedXPos) {
              totalDx += abs(totalDy)
              slidSuccessfully = true
            } else if (!yBlockedXNeg) {
              totalDx -= abs(totalDy)
              slidSuccessfully = true
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
          if (!slidSuccessfully) {
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