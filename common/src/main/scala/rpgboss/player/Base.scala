package rpgboss.player
import scala.concurrent._
import scala.concurrent.duration.Duration
import rpgboss.lib.TweenUtils
import scala.collection.mutable.ArrayBuffer
import rpgboss.lib.ThreadChecked

/** Keeps track of a queue of mutation operations on a 'T'.
 */
class MutateQueue[T](mutatedObject: T) extends ThreadChecked {
  private val queue =
    new collection.mutable.Queue[MutateQueueItem[T]]

  def length = {
    assertOnBoundThread()
    queue.length
  }

  override def toString() = {
    assertOnBoundThread()
    queue.toString()
  }

  def isEmpty = {
    assertOnBoundThread()
    queue.isEmpty
  }
  def enqueue(item: MutateQueueItem[T]) = {
    assertOnBoundThread()
    queue.enqueue(item)
  }

  def runQueueItem(delta: Float): Unit = {
    assertOnBoundThread()
    assert(!isEmpty)

    queue.head.update(delta, mutatedObject)
    if (queue.head.isFinished)
      queue.dequeue()
  }
}

trait MutateQueueItem[T] extends FinishableByPromise {
  def update(delta: Float, mutated: T)
}

trait Finishable {
  def isFinished: Boolean
  def awaitFinish(): Int
}

trait FinishableStoppable extends Finishable {
  def stop(): Unit
}

trait Tweener[T] {
  def get(): T
  def set(newValue: T): Unit

  var _endValue: T
  var _startValue: T

  def interpolate(startValue: T, endValue: T, alpha: Float): T

  private var _age = 0.0f
  private var _done = true

  private var _tweenDuration = 0f

  private val _doneCallbacks = ArrayBuffer[() => Unit]()

  def done = _done

  def tweenTo(endValue: T, duration: Float) = {
    _age = 0f
    _done = false

    _startValue = get()
    _endValue = endValue

    _tweenDuration = duration
    flushCallbacks()
  }

  def flushCallbacks() = {
    _doneCallbacks.foreach(_())
    _doneCallbacks.clear()
  }

  def runAfterDone(closure: () => Unit): Unit = {
    if (_done)
      closure()
    else
      _doneCallbacks.append(closure)
  }

  def update(delta: Float): Unit = {
    if (_done)
      return

    _age += delta
    if (_age >= _tweenDuration) {
      set(_endValue)
      flushCallbacks()

      _done = true
      return
    }

    val alpha = TweenUtils.tweenAlpha(0.0f, _tweenDuration, _age)
    set(interpolate(_startValue, _endValue, alpha))
  }

  def finish() = {
    set(_endValue)
    _done = true
    flushCallbacks()
  }
}

class FloatTweener(getF: () => Float, setF: Float => Unit)
  extends Tweener[Float] {

  var _startValue = 0f
  var _endValue = 0f

  override def get() = getF()
  override def set(newValue: Float) = setF(newValue)

  override def interpolate(startValue: Float, endValue: Float, alpha: Float) =
    TweenUtils.tweenFloat(alpha, startValue, endValue)
}

trait FinishableByPromise extends Finishable {
  private val finishPromise = Promise[Int]()

  override def isFinished = finishPromise.isCompleted
  def finish() = finishWith(0)
  def finishWith(result: Int) = finishPromise.success(result)
  override def awaitFinish() = Await.result(finishPromise.future, Duration.Inf)
}

object DummyFinished extends Finishable {
  override def isFinished = true
  override def awaitFinish() = 0
}