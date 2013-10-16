package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import rpgboss.model.MapLoc
import java.util.Date
import org.lwjgl.opengl.Display

/** Can hold a delegate MyGame that can be replaced for running multiple tests.
 */
class TestGameContainer extends ApplicationListener {
  var childGame: Option[MyGame] = None
  
  // Can be called on any thread, since it will post to the Gdx thread.
  def replace(game: MyGame) = {
    Gdx.app.postRunnable(new Runnable() {
      def run() = {
        childGame.map(_.pause())
        childGame.map(_.dispose())
        childGame = Some(game)
        childGame.map(_.create())
      }
    })
  }
  
  def create() = {}
  
  def dispose() = {
    childGame.map(_.dispose())
  }
  
  def pause() = {
    childGame.map(_.pause())
  }
  
  def render() = {
    childGame.map(_.render())
  }
  
  def resize(x: Int, y: Int) = {
    childGame.map(_.resize(x, y))
  }
  
  def resume() = {
    childGame.map(_.resume())
  }
}

object TestPlayer {
  val container = new TestGameContainer
  var app: LwjglApplication = null
  
  def launch(game: MyGame) = {
    val conf = new LwjglApplicationConfiguration();
    conf.title = game.project.data.title;
    conf.width = 32 * 20;
    conf.height = 32 * 15;
    conf.useGL20 = true;
    conf.forceExit = false;

    if (app == null) {
      app = new LwjglApplication(container, conf)
    }
    
    container.replace(game)
  }
}