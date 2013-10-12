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
import org.lwjgl.opengl.Display

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
  
  def setup()
  def runTest()
  
  override def beginGame() = {
    setup()
    
    future {
      runTest()
      val app = Gdx.app.asInstanceOf[LwjglApplication]
      app.stop()
      Gdx.app = null
      Gdx.graphics = null
      Gdx.audio = null
      Gdx.files = null
      Gdx.net = null
      Display.destroy()
      w.dismiss()
    }
  }
}