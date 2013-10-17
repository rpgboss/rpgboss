package rpgboss.player

import scala.concurrent._
import scala.concurrent.duration.Duration
import com.badlogic.gdx.math.Vector2

/** Controls where the camera is pointed. Accessed only on the Gdx thread.
 *  
 *  The camera is implicitly locked to the player. If however, there are moves
 *  in the move queue, it will execute those before re-locking to the player.
 *  
 *  Callers must move the camera back to the player if they don't want the
 *  camera to jerk afterwards.
 */
class Camera(game: MyGame) {
  private var _x: Float = 0f
  private var _y: Float = 0f
  
  private var _speed: Float = 2f // tiles per second
  
  private val moveQueue = new collection.mutable.Queue[CameraMove]
  
  class CameraMove(dx: Float, dy: Float) {
    private val _remaining = new Vector2(dx, dy)
    private val finishPromise = Promise[Int]()
    
    def remainingLength = _remaining.len()
    def subtractTravel(travel: Vector2) = _remaining.sub(travel)
    def remainingDx = _remaining.x
    def remainingDy = _remaining.y
    def remaining = _remaining.cpy()
    
    def finish() = finishPromise.success(0)
    def awaitDone() = Await.result(finishPromise.future, Duration.Inf)
  }
  
  def state = game.state
  
  def x = _x
  def y = _y
  
  def setSpeed(speed: Float) = 
    _speed = speed
  
  def update(delta: Float) = {
    if (moveQueue.isEmpty) {
      _x = game.state.playerEntity.x
      _y = game.state.playerEntity.y
    } else if (!moveQueue.isEmpty) {
      val maxTravel = delta * _speed
      val move = moveQueue.head
      
      if (move.remainingLength <= maxTravel) {
        _x += move.remainingDx
        _y += move.remainingDy
        moveQueue.dequeue()
        move.finish()
      } else {
        val travel = move.remaining.nor().scl(maxTravel)
        _x += travel.x
        _y += travel.y
        move.subtractTravel(travel)
      }
    }
  }
  
  def enqueueMove(dx: Float, dy: Float): CameraMove = {
    val move = new CameraMove(dx, dy)
    moveQueue.enqueue(move)
    return move
  }
  
  def setCameraLoc(x: Float, y: Float) = {
    _x = x
    _y = y
  }
}