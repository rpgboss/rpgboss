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

trait EntityLike {
  def x: Float
  def y: Float
  def speed: Float
}

object Entity {
  def defaultEntitySpeed = 4.0f

  def getDirection(vec: Vector2): Int = {
    if (math.abs(vec.x) > math.abs(vec.y))
      if (vec.x > 0)
        SpriteSpec.Directions.EAST
      else
        SpriteSpec.Directions.WEST
    else {
      if (vec.y > 0)
        SpriteSpec.Directions.SOUTH
      else
        SpriteSpec.Directions.NORTH
    }
  }
}

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
abstract class Entity(
  spritesets: Map[String, Spriteset],
  mapAndAssetsOption: Option[MapAndAssets],
  allEntities: collection.Map[Int, Entity],
  var x: Float = 0f,
  var y: Float = 0f,
  var dir: Int = SpriteSpec.Directions.SOUTH,
  var initialSprite: Option[SpriteSpec] = None) extends EntityLike {

  def playerEntity = allEntities(EntitySpec.playerEntityId)

  def height: Int
  def zPriority = y
  def trigger: Int

  // TODO: Remove this ghetto RTTI if possible.
  def isPlayer: Boolean = false
  def collisionOn: Boolean = true

  def inVehicle = false
  def inVehicleId = -1

  /**
   * Called when a player activates the event with a button.
   */
  def activate(activatorsDirection: Int): Option[Finishable] = None

  private val moveQueue = new MutateQueue(this)
  def moveQueueEmpty = moveQueue.isEmpty
  protected var movesEnqueued: Long = 0

  var speed: Float = Entity.defaultEntitySpeed
  private var isMovingVar = false
  private var movingSince: Long = 0
  private var msPerStep = 128

  var spriteset: Spriteset = null
  var spriteIdx: Int = -1

  var graphicW: Float = 0f
  var graphicH: Float = 0f

  var stillStep = SpriteSpec.Steps.STILL
  var boundingBoxHalfsize = 0.5f

  /**
   * Automatically subtracted from boundingBoxHalfsizes to allow for easier
   * movement of events and the player past each other.
   */
  def boundingBoxHalfsizeTolerance = 0.1f

  def isMoving() = isMovingVar

  val tintColor = Color.WHITE.cpy()
  def setTintColor(color: Color) = {
    tintColor.set(color)
  }

  def setBoundingBoxHalfsize(halfsizeArg: Float) = {
    // Normalize to prevent bounding boxes too large to be used on tile map
    val halfsize = math.min((2.0f - collisionDeltas) / 2.0f, halfsizeArg)

    boundingBoxHalfsize = halfsize - boundingBoxHalfsizeTolerance
  }

  def getBoundingBox() = {
    BoundingBox(x - boundingBoxHalfsize, y - boundingBoxHalfsize,
      x + boundingBoxHalfsize, y + boundingBoxHalfsize)
  }

  /**
   * Returns a tuple (ux, uy) representing a unit vector of the face direction.
   */
  def getDirectionUnitVector(): (Float, Float) = {
    dir match {
      case SpriteSpec.Directions.NORTH => (0f, -1f)
      case SpriteSpec.Directions.SOUTH => (0f, 1f)
      case SpriteSpec.Directions.EAST => (1f, 0f)
      case SpriteSpec.Directions.WEST => (-1f, 0f)
    }
  }

  /**
   * Returns a tuple of (blocked, reroute number).
   */
  def getMapCollisions(dxArg: Float, dyArg: Float,
                       inVehicle: Boolean) : (Boolean, Int) = {
    mapAndAssetsOption map { mapAndAssets =>
      mapAndAssets.getCollisions(this, x, y, dxArg, dyArg, inVehicle)
    } getOrElse (true, 0)
  }

  /**
   * Returns if the entity can freely stand at current position modified by
   * dx, dy. This 'pretends' that the player is not in a vehicle.
   */
  def canStandAt(dx: Float, dy: Float): Boolean = {
    val (blocked, _) = getMapCollisions(dx, dy, inVehicle = false)
    if (blocked)
      return false

    val (_, evtBlocked) = getTouchingAndBlockingEvents(dx, dy)
    if (evtBlocked)
      return false

    return true
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

    // Don't modify the player's direction on sprite update.
    if (!isPlayer)
      dir = s.dir

    stillStep = s.step
  } getOrElse {
    spriteset = null
    setBoundingBoxHalfsize((1.0f - collisionDeltas) * 0.5f)
  }

  /**
   * Finds all events with which this dxArg and dyArg touches. Excludes self.
   */
  def getAllEventTouches(dxArg: Float, dyArg: Float) = {
    val boundingBox = getBoundingBox().offsetted(dxArg, dyArg)
    allEntities.values.filter(npc => {
      npc.getBoundingBox().contains(boundingBox)
    }).filter(_ != this)
  }

  def enqueueMove(move: MutateQueueItem[Entity]) = {
    moveQueue.enqueue(move)
    movesEnqueued += 1
  }

  def moveEntity(vec: Vector2, affixDirection: Boolean): EntityMove = {
    if (vec.x != 0 || vec.y != 0) {
      if (!affixDirection)
        dir = Entity.getDirection(vec)

      val move = EntityMove(vec)
      enqueueMove(move)
      return move
    }
    null
  }

  def randomStep() = {
    var r = scala.util.Random
    var randomStep = Math.round(r.nextFloat*4)

    var vec = new Vector2(0,0)

    if (randomStep==1) {
      vec = new Vector2(1,0)
    } else if (randomStep==2) {
      vec = new Vector2(0,1)
    } else if (randomStep==3) {
      vec = new Vector2(-1,0)
    } else if (randomStep==4) {
      vec = new Vector2(0,-1)
    }
    dir = Entity.getDirection(vec)
    val move = EntityMove(vec)
    enqueueMove(move)
  }

  def moveAwayFromEntity(playerEntity:Entity, affixDirection: Boolean) = {

    var xDiff = x - playerEntity.x
    var yDiff = y - playerEntity.y
    var xMove = 0
    var yMove = 0

    if(xDiff > 0) {
      xMove = 1
    }
    if(xDiff < 0) {
      xMove = -1
    }
    if(yDiff > 0) {
      yMove = 1
    }
    if(yDiff < 0) {
      yMove = -1
    }

    var vec = new Vector2(xMove,yMove)

    if (!affixDirection)
      dir = Entity.getDirection(vec)

    val move = EntityMove(vec)
    enqueueMove(move)
  }

  /**
   * This method is called when this event collides against others while
   * it is moving.
   */
  def touchEntities(touchedEntities: Iterable[Entity]) = {}

  def onButton() = {}

  /**
   * Find closest entity to this one given an offset dx and dy.
   */
  def closest[T <: Entity](entities: Iterable[T], dx: Float = 0,
      dy: Float = 0) = {
    assert(!entities.isEmpty)
    entities.minBy(e => math.abs(e.x - (x + dx)) +
                        math.abs(e.y - (y + dy)))
  }

  /**
   * Returns the set of events that this entity is touching, and the smaller
   * set that's blocking it.
   */
  def getTouchingAndBlockingEvents(dx: Float, dy: Float) = {
    val evtsTouched = getAllEventTouches(dx, dy).filter(_ != this)

    val evtBlocking =
      collisionOn && height == EventHeight.SAME.id &&
      evtsTouched.exists(_.height == EventHeight.SAME.id)

    (evtsTouched, evtBlocking)
  }

  def update(delta: Float, eventsEnabled: Boolean): Unit = {
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

  def dispose(): Unit
}

case class BoundingBox(minX: Float, minY: Float, maxX: Float, maxY: Float) {
  def contains(o: BoundingBox) =
    o.maxX >= minX && o.minX <= maxX && o.maxY >= minY && o.minY <= maxY

  def contains(x: Float, y: Float) =
    minX <= x && x <= maxX && minY <= y && y <= maxY

  def offsetted(dx: Float, dy: Float) =
    copy(minX + dx, minY + dy, maxX + dx, maxY + dy)
}

case class EntityMove(vec: Vector2)
  extends MutateQueueItem[Entity] {
  val remainingTravel = vec

  def update(delta: Float, entity: Entity) = {
    import math._

    val desiredThisFrame =
      remainingTravel.cpy().nor().scl(min(entity.speed * delta, remainingTravel.len()))

    val travelledThisFrame = new Vector2()

    val maxIterations =
      (desiredThisFrame.len() / entity.collisionDeltas).toInt + 1

    var travelDoneThisFrame = false
    var i = 0
    while (i < maxIterations && !travelDoneThisFrame && !isFinished) {
      val lengthThisIteration = min(
          entity.collisionDeltas,
          desiredThisFrame.len() - travelledThisFrame.len())

      val movementThisIteration =
        desiredThisFrame.cpy().nor().scl(lengthThisIteration)
      val dx = movementThisIteration.x
      val dy = movementThisIteration.y

      var movedThisLoop = false

      val (evtsTouched, evtBlocking) =
        entity.getTouchingAndBlockingEvents(dx, dy)

      entity.touchEntities(evtsTouched.toSet)

      // Move along x
      if (!evtBlocking && dx != 0) {
        // Determine collisions in x direction on the y-positive corner
        // and the y negative corner of the bounding box
        val (mapBlocked, mapReroute) =
          entity.getMapCollisions(dx, 0, inVehicle = entity.inVehicle)

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
      if (!evtBlocking && dy != 0) {
        // Determine collisions in x direction on the y-positive corner
        // and the y negative corner of the bounding box
        val (mapBlocked, mapReroute) =
          entity.getMapCollisions(0, dy, inVehicle = entity.inVehicle)

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

      i += 1
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