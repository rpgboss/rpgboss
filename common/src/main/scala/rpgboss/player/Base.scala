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
    
    if (queue.head.isDone())
      queue.dequeue()
  }
  
}

trait MutateQueueItem[T] {
  private val finishPromise = Promise[Int]()
  
  def update(delta: Float, mutated: T)
  
  def isDone() = finishPromise.isCompleted
  def finish() = finishPromise.success(0)
  def awaitDone() = Await.result(finishPromise.future, Duration.Inf)
}