package rpgboss.player
import scala.concurrent._
import scala.concurrent.duration.Duration

/** Keeps track of a queue of mutation operations on a 'T'.
 */
class MutateQueue[T](mutatedObject: T) {
  val queue = new collection.mutable.Queue[MutateQueueItem[T]]

  def isEmpty = queue.isEmpty
  def enqueue(item: MutateQueueItem[T]) = queue.enqueue(item)
  def dequeue() = queue.dequeue()

  // Runs an item out of the queue if it exists.
  def runQueueItem(delta: Float): Unit = {
    if(queue.isEmpty)
      return

    val action = queue.head

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
  def awaitFinish(): Unit
}

trait FinishableByPromise extends Finishable {
  private val finishPromise = Promise[Int]()

  override def isFinished = finishPromise.isCompleted
  def finish() = finishPromise.success(0)
  override def awaitFinish() = Await.result(finishPromise.future, Duration.Inf)
}

object DummyFinished extends Finishable {
  override def isFinished = true
  override def awaitFinish() = Unit
}