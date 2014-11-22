package rpgboss.player

import scala.concurrent._
import scala.concurrent.duration.Duration
import com.badlogic.gdx.math.Vector2

case class CameraInfo(x: Float, y: Float, speed: Float, moveQueueLength: Int)

/** Controls where the camera is pointed. Accessed only on the Gdx thread.
 *
 *  The camera is implicitly locked to the player. If however, there are moves
 *  in the move queue, it will execute those before re-locking to the player.
 *
 *  Callers must move the camera back to the player if they don't want the
 *  camera to jerk afterwards.
 */
class MapCamera(game: RpgGame) {
  var x: Float = 0f
  var y: Float = 0f
  var speed: Float = 2f // tiles per second
  val moveQueue = new MutateQueue(MapCamera.this)

  def info = CameraInfo(x, y, speed, moveQueue.queue.length)

  def update(delta: Float, x: Float, y: Float) = {
    if (moveQueue.isEmpty) {
      this.x = x
      this.y = y
    } else {
      moveQueue.runQueueItem(delta)
    }
  }

  def enqueueMove(dx: Float, dy: Float): MapCameraMove = {
    val move = new MapCameraMove(dx, dy)
    moveQueue.enqueue(move)
    return move
  }
}

class MapCameraMove(dx: Float, dy: Float) extends MutateQueueItem[MapCamera] {
  private val _remaining = new Vector2(dx, dy)

  def update(delta: Float, c: MapCamera) = {
    val maxTravel = delta * c.speed
    if (_remaining.len() <= maxTravel) {
      c.x += _remaining.x
      c.y += _remaining.y
      finish()
    } else {
      val travel = _remaining.nor().scl(maxTravel)
      c.x += travel.x
      c.y += travel.y
      _remaining.sub(travel)
    }
  }
}