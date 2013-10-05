package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
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
    conf.forceExit = true;

    new LwjglApplication(game, conf);
  }
}

abstract class TestGame(gamepath: File) 
  extends MyGame(gamepath) {
  
  private val finishPromise = Promise[Boolean]()
  private var timeout = 10.0f // seconds

  def runTest()
  
  // Returns whether or not the test finished successfully
  def awaitFinish() = Await.result(finishPromise.future, Duration.Inf)
  
  override def beginGame() = {
    future {
      runTest()
      //Gdx.app.exit()
      //finishPromise.success(true)
    }
  }
  
  override def render() = {
    super.render()
    
    timeout -= Gdx.graphics.getDeltaTime()
    if (timeout < 0) {
      //Gdx.app.exit()
      finishPromise.success(false)
    }
  }
}