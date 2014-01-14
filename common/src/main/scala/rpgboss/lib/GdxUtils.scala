package rpgboss.lib

import com.badlogic.gdx.Gdx
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object GdxUtils {
  /**
   * Run the following on the GUI thread
   */
  def syncRun[T](op: => T): T = {
    val promise = Promise[T]
    val runnable = new Runnable() {
      def run() = {
        promise.success(op)
      }
    }
    Gdx.app.postRunnable(runnable)
    Await.result(promise.future, Duration.Inf)
  }
  
  /**
   * Run the following on the GUI thread
   */
  def asyncRun[T](op: => T): Unit = {
    val runnable = new Runnable() {
      def run() = {
        op
      }
    }
    Gdx.app.postRunnable(runnable)
  }
}