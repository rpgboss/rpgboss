package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import rpgboss.model.MapLoc
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.Date

object TestPlayer {
  def launch(game: MyGame) = {
    val conf = new LwjglApplicationConfiguration();
    conf.title = game.project.data.title;
    conf.width = 32 * 20;
    conf.height = 32 * 15;
    conf.useGL20 = true;
    conf.forceExit = false;

    new LwjglApplication(game, conf);
  }
}

abstract class TestGame(gamepath: File, w: Waiter) 
  extends MyGame(gamepath) {
  
  private val exitPromise = Promise[Int]()
  def setup()
  def runTest()
  
  def awaitExit() = Await.result(exitPromise.future, Duration.Inf)
  
  override def beginGame() = {
    setup()
    
    future {
      runTest()
      Gdx.app.exit()
      w.dismiss()
    }
  }
  
  override def dispose() = {
    super.dispose()
    exitPromise.success(0)
  }
  
  override def render() = {
    super.render()
  }
}