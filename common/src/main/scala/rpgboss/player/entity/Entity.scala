package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import rpgboss.model.SpriteSpec.Steps.TOTALSTEPS
import rpgboss.player.RpgGame
import scala.math.abs
import scala.math.min
import scala.math.signum
import rpgboss.model.event.EventHeight
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import rpgboss.player._
import com.badlogic.gdx.graphics.Color

/**
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
 * Bottom edge length is boundBoxTiles.
 */
class Entity(
  spritesets: Map[String, Spriteset],
  mapAndAssetsOption: Option[MapAndAssets],
  eventEntities: collection.Map[Int, EventEntity],
  var x: Float = 0f,
  var y: Float = 0f,
  var dir: Int = SpriteSpec.Directions.SOUTH,
  var initialSprite: Option[SpriteSpec] = None) {

  def zPriority = y

  private val moveQueue = new MutateQueue(this)
  protected var movesEnqueued: Long = 0

  var speed: Float = 3.0f
  private var isMovingVar = false
  private var movingSince: Long = 0
  private var msPerStep = 128

  var spriteset: Spriteset = null
  var spriteIdx: Int = -1

  var graphicW: Float = 0f
  var graphicH: Float = 0f

  var stillStep = SpriteSpec.Steps.STILL
  var boundingBoxHalfsize = 0.5f

  def isMoving() = isMovingVar

  val tintColor = Color.WHITE.cpy()
  def setTintColor(color: Color) = {
    tintColor.set(color)
  }

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
    mapAndAssetsOption map { mapAndAssets =>
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
    spriteset = spritesets(s.name)
    spriteIdx = s.spriteIndex

    graphicW = (spriteset.tileW.toDouble / Tileset.tilesize).toFloat
    graphicH = (spriteset.tileH.toDouble / Tileset.tilesize).toFloat
    // Minus the delta to allow events to fit into tiles easily
    setBoundingBoxHalfsize((graphicW - collisionDeltas) * 0.5f)
    dir = s.dir
    stillStep = s.step
  } getOrElse {
    spriteset = null
    setBoundingBoxHalfsize((1.0f - collisionDeltas) * 0.5f)
  }

  /**
   * This calculates the touches based only on the center
   */
  def getAllEventCenterTouches(dxArg: Float, dyArg: Float) = {
    eventEntities.values.filter(npc => {
      npc.getBoundingBox().contains(x + dxArg, y + dyArg)
    })
  }

  /**
   * Finds all events with which this dxArg and dyArg touches
   */
  def getAllEventTouches(dxArg: Float, dyArg: Float) = {
    val boundingBox = getBoundingBox().offsetted(dxArg, dyArg)
    eventEntities.values.filter(npc => {
      npc.getBoundingBox().contains(boundingBox)
    })
  }

  def enqueueMove(move: MutateQueueItem[Entity]) = {
    moveQueue.enqueue(move)
    movesEnqueued += 1
  }

  /**
   * This method is called when event collides against another event during
   * movement.
   */
  def eventTouchCallback(touchedNpcs: Iterable[EventEntity]) = {}

  def update(delta: Float) = {
    if (!moveQueue.isEmpty) {
      if (!isMovingVar) {
        isMovingVar = true
        movingSince = System.currentTimeMillis()
      }
      moveQueue.runQueueItem(delta)
    } else {
      isMovingVar = false
    }
  }

  def render(batch: SpriteBatch, atlasSprites: TextureAtlas) = {
    if (spriteset != null) {
      /*
       * Given the definition of the position (see beginning of the file),
       * calculate the top-left corner of the graphic we draw.
       * We use top-left because we have flipped the y-axis in libgdx to match
       * the map coordinates we use.
       */
      val dstOriginX: Float = x - graphicW / 2.0f
      val dstOriginY: Float = y - graphicH + graphicW / 2

      spriteset.renderSprite(
        batch, atlasSprites,
        spriteIdx, dir, currentStep(),
        dstOriginX, dstOriginY, graphicW, graphicH)
      batch.setColor(tintColor)
      spriteset.renderSprite(
        batch, atlasSprites,
        spriteIdx, dir, currentStep(),
        dstOriginX, dstOriginY, graphicW, graphicH)
      batch.setColor(Color.WHITE)
    }
  }
}

case class BoundingBox(minX: Float, minY: Float, maxX: Float, maxY: Float) {
  def contains(o: BoundingBox) =
    o.maxX >= minX && o.minX <= maxX && o.maxY >= minY && o.minY <= maxY

  def contains(x: Float, y: Float) =
    minX <= x && x <= maxX && minY <= y && y <= maxY

  def offsetted(dx: Float, dy: Float) =
    copy(minX + dx, minY + dy, maxX + dx, maxY + dy)
}

case class EntityMove(totalDx: Float, totalDy: Float)
  extends MutateQueueItem[Entity] {
  val remainingTravel = new Vector2(totalDx, totalDy)

  def update(delta: Float, entity: Entity) = {
    import math._

    val desiredThisFrame =
      remainingTravel.cpy().nor().scl(min(entity.speed * delta, remainingTravel.len()))

    val travelledThisFrame = new Vector2()

    var travelDoneThisFrame = false
    while (!travelDoneThisFrame && !isFinished) {
      val lengthThisIteration = min(
          entity.collisionDeltas,
          desiredThisFrame.len() - travelledThisFrame.len())
      val movementThisIteration =
        desiredThisFrame.cpy().nor().scl(lengthThisIteration)
      val dx = movementThisIteration.x
      val dy = movementThisIteration.y

      var movedThisLoop = false

      val evtsTouchedX =
        entity.getAllEventCenterTouches(dx, 0).filter(_ != entity)
      val evtsTouchedY =
        entity.getAllEventCenterTouches(0, dy).filter(_ != entity)

      val evtsTouchedSet = evtsTouchedX.toSet ++ evtsTouchedY.toSet
      entity.eventTouchCallback(evtsTouchedSet)

      val evtBlockingX = evtsTouchedX.exists(_.height == EventHeight.SAME.id)
      val evtBlockingY = evtsTouchedY.exists(_.height == EventHeight.SAME.id)

      // Move along x
      if (!evtBlockingX && desiredThisFrame.x != 0) {
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
      if (!evtBlockingY && desiredThisFrame.y != 0) {
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
      } else {
        travelDoneThisFrame = true
        finish()
      }
    }

    remainingTravel.sub(desiredThisFrame)

    if (remainingTravel.len() < entity.collisionDeltas && !isFinished)
      finish()
  }
}

case class EntityFaceDirection(direction: Int) extends MutateQueueItem[Entity] {
  def update(delta: Float, entity: Entity) = {
    entity.dir = direction
    finish()
  }
}