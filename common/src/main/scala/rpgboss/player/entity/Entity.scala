package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import rpgboss.model.SpriteSpec.Steps.TOTALSTEPS
import rpgboss.player.MyGame
import scala.math.abs
import scala.math.min
import scala.math.signum
import rpgboss.model.event.EventHeight
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
  val game: MyGame,
  var x: Float = 0f,
  var y: Float = 0f,
  var dir: Int = SpriteSpec.Directions.SOUTH,
  var initialSprite: Option[SpriteSpec] = None) {

  val moveQueue = new collection.mutable.SynchronizedQueue[EntityMoveTrait]
  var movesEnqueued: Long = 0
  
  var speed: Float = 3.0f
  private var isMovingVar = false
  private var movingSince: Long = 0
  private var msPerStep = 128

  var spriteset: Spriteset = null
  var spriteIdx: Int = -1
  var graphicWTiles: Float = 0f
  var graphicHTiles: Float = 0f

  var stillStep = SpriteSpec.Steps.STILL
  var boundingBoxHalfsize = 0.5f
  
  def isMoving() = isMovingVar

  def setBoundingBoxHalfsize(halfsizeArg: Float) = {
    // Normalize to prevent bounding boxes too large to be used on tile map
    val halfsize = math.min((2.0f - collisionDeltas) / 2.0f, halfsizeArg)

    boundingBoxHalfsize = halfsize
  }

  def getBoundingBox() = {
    BoundingBox(x - boundingBoxHalfsize, y - boundingBoxHalfsize,
      x + boundingBoxHalfsize, y + boundingBoxHalfsize)
  }

  def getMapCollisions(dxArg: Float, dyArg: Float) : (Boolean, Int) = {
    game.state.mapAndAssetsOption map { mapAndAssets =>
      mapAndAssets.getCollisions(this, x, y, dxArg, dyArg)
    } getOrElse (true, 0)
  }

  setBoundingBoxHalfsize(0.5f)

  def collisionDeltas = 0.05f

  def currentStep() = if (isMovingVar) {
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
  def getAllEventCenterTouches(dxArg: Float, dyArg: Float) = {
    game.state.eventEntities.values.filter(npc => {
      npc.getBoundingBox().contains(x + dxArg, y + dyArg)
    })
  }

  def getAllEventTouches(dxArg: Float, dyArg: Float) = {
    val boundingBox = getBoundingBox()
    game.state.eventEntities.values.filter(npc => {
      npc.getBoundingBox().contains(boundingBox)
    })
  }

  def enqueueMove(move: EntityMoveTrait) = {
    moveQueue.enqueue(move)
    movesEnqueued += 1
    // game.logger.info("enqueueMove")
  }
  def dequeueMove() = {
    moveQueue.dequeue()
    // game.logger.info("dequeueMove")
  }
  
  /**
   * This method is called when event collides against another event during
   * movement.
   */
  def eventTouchCallback(touchedNpcs: Iterable[EventEntity])

  def update(delta: Float) = {
    var moveQueueUpdateDone = false
    while (!moveQueueUpdateDone && !moveQueue.isEmpty) {
      if (!isMovingVar) {
        isMovingVar = true
        movingSince = System.currentTimeMillis()
      }
      moveQueue.head.update(this, delta)
      
      if (moveQueue.head.isDone())
        dequeueMove()
      else
        moveQueueUpdateDone = true
    }
    
    if (moveQueue.isEmpty)
      isMovingVar = false
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

  def contains(x: Float, y: Float) =
    minX <= x && x <= maxX && minY <= y && y <= maxY

  def offseted(dx: Float, dy: Float) =
    copy(minX + dx, minY + dy, maxX + dx, maxY + dy)
}

trait EntityMoveTrait {
  def update(entity: Entity, delta: Float): Unit
  
  private val finishPromise = Promise[Int]()
  
  def isDone(): Boolean = finishPromise.isCompleted
  def finish() = finishPromise.success(0)
  def awaitDone() = Await.result(finishPromise.future, Duration.Inf)
}

case class EntityMove(totalDx: Float, totalDy: Float)
  extends EntityMoveTrait {
  val remainingTravel = new Vector2(totalDx, totalDy)
  
  def update(entity: Entity, delta: Float) = {
    import math._

    val desiredThisFrame = 
      remainingTravel.cpy().nor().scl(min(entity.speed * delta, remainingTravel.len()))

    val travelledThisFrame = new Vector2()

    var travelDoneThisFrame = false
    while (!travelDoneThisFrame && !isDone()) {
      val lengthThisIteration = min(
          entity.collisionDeltas, 
          desiredThisFrame.len() - travelledThisFrame.len())
      val movementThisIteration = 
        desiredThisFrame.cpy().nor().scl(lengthThisIteration)
      val dx = movementThisIteration.x
      val dy = movementThisIteration.y

      var movedThisLoop = false
      
      val evtsTouched = 
        entity.getAllEventCenterTouches(dx, dy).filter(_ != entity)
      entity.eventTouchCallback(evtsTouched)
      val evtBlocking =
        evtsTouched.exists(_.evtState.height == EventHeight.SAME.id)

      // Move along x
      if (!evtBlocking && desiredThisFrame.x != 0) {
        // Determine collisions in x direction on the y-positive corner
        // and the y negative corner of the bounding box
        val (mapBlocked, mapReroute) = entity.getMapCollisions(dx, 0)

        // Conventional movement if it succeeeds
        if (!mapBlocked) {
          movedThisLoop = true
          travelledThisFrame.x += dx
          entity.x += dx
        } else if (desiredThisFrame.y == 0) {
          // Conventional movement blocked. Try sliding perpendicularly
          if (mapReroute != 0) {
            movedThisLoop = true
            val yReroute = mapReroute * abs(dx)
            entity.y += yReroute
            travelledThisFrame.y += yReroute
          }
        }
      }

      // Move along y
      if (!evtBlocking && desiredThisFrame.y != 0) {
        // Determine collisions in x direction on the y-positive corner
        // and the y negative corner of the bounding box
        val (mapBlocked, mapReroute) = entity.getMapCollisions(0, dy)

        // Conventional movement if it succeeeds
        if (!mapBlocked) {
          movedThisLoop = true
          travelledThisFrame.y += dy
          entity.y += dy
        } else if (desiredThisFrame.x == 0) {
          // Conventional movement blocked. Try sliding perpendicularly
          if (mapReroute != 0) {
            movedThisLoop = true
            val xReroute = mapReroute * abs(dy)
            entity.x += xReroute
            travelledThisFrame.x += xReroute
          }
        }
      }

      // Was able to move conventionally
      if (movedThisLoop) {
        // Check if we are done travelling by distance measure
        if (travelledThisFrame.len() >= desiredThisFrame.len()) {
          travelDoneThisFrame = true
        }
        //entity.game.logger.info("Moved to %f, %f".format(entity.x, entity.y))
      } else {
        travelDoneThisFrame = true
        finish()
      }
    }
    
//    entity.game.logger.info("desired / travel : " + desiredThisFrame.toString()
//                            + " " + travelledThisFrame.toString())
    
    remainingTravel.sub(desiredThisFrame)
    
    if (remainingTravel.len() < entity.collisionDeltas && !isDone())
      finish()
  }
}

case class EntityFaceDirection(direction: Int) extends EntityMoveTrait {
  def update(entity: Entity, delta: Float) = {
    entity.dir = direction
    finish()
  }
}