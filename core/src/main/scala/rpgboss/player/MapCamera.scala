package rpgboss.player

import scala.concurrent._
import scala.concurrent.duration.Duration
import com.badlogic.gdx.math.Vector2
import rpgboss.lib.Utils
import rpgboss.model.resource.RpgMapMetadata
import rpgboss.player.entity.EntityLike

case class CameraInfo(x: Float, y: Float, speed: Float, moveQueueLength: Int)

/**
 * Controls where the camera is pointed. Accessed only on the Gdx thread.
 *
 *  The camera is implicitly locked to the player. If however, there are moves
 *  in the move queue, it will execute those before re-locking to the player.
 *
 *  Callers must move the camera back to the player if they don't want the
 *  camera to jerk afterwards.
 */
class MapCamera {
  var x: Float = 0f
  var y: Float = 0f
  var speed: Float = 5f // tiles per second
  val moveQueue = new MutateQueue(MapCamera.this)

  def info = CameraInfo(x, y, speed, moveQueue.length)

  def update(delta: Float, trackedEntity: Option[EntityLike],
             forceSnapToEntity: Boolean, mapMetadata: RpgMapMetadata,
             screenWTiles: Float, screenHTiles: Float): Unit = {
    if (!moveQueue.isEmpty) {
      moveQueue.runQueueItem(delta)
      return
    }

    if (trackedEntity.isEmpty)
      return

    var desiredX = trackedEntity.get.x
    var desiredY = trackedEntity.get.y

    if (screenWTiles >= mapMetadata.xSize) {
      desiredX = mapMetadata.xSize.toFloat / 2
    } else {
      desiredX = Utils.clamped(
          desiredX, screenWTiles / 2, mapMetadata.xSize - screenWTiles / 2)
    }

    if (screenHTiles >= mapMetadata.ySize) {
      desiredY = mapMetadata.ySize.toFloat / 2
    } else {
      desiredY = Utils.clamped(
          desiredY, screenHTiles / 2, mapMetadata.ySize - screenHTiles / 2)
    }

    if (forceSnapToEntity) {
      this.x = desiredX
      this.y = desiredY
    } else {
      // Move towards the tracked entity.
      val travel = new Vector2(desiredX - x, desiredY - y)
      travel.clamp(0, delta * trackedEntity.get.speed)

      this.x += travel.x
      this.y += travel.y
    }
  }

  def enqueueMove(dx: Float, dy: Float, duration: Float): MapCameraMove = {
    val vec = new Vector2(dx, dy)
    val move = new MapCameraMove(vec, duration)
    moveQueue.enqueue(move)
    return move
  }
}

class MapCameraMove(vec: Vector2, duration: Float)
  extends MutateQueueItem[MapCamera] {
  private val _remaining = vec

  val speed = vec.len() / duration

  def update(delta: Float, c: MapCamera) = {
    val maxTravel = delta * c.speed
    if (_remaining.len() <= maxTravel) {
      c.x += _remaining.x
      c.y += _remaining.y
      finish()
    } else {
      val travel = _remaining.cpy().nor().scl(maxTravel)
      c.x += travel.x
      c.y += travel.y
      _remaining.sub(travel)
    }
  }
}