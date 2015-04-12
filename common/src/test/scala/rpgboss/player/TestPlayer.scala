package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration

/** Can hold a delegate MyGame that can be replaced for running multiple tests.
 */
class TestGameContainer extends ApplicationListener {
  var childGame: Option[RpgGame] = None

  // Can be called on any thread, since it will post to the Gdx thread.
  def replace(game: RpgGame) = {
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
  private var lwjglApp: LwjglApplication = null
  private var headlessApp: HeadlessApplication = null

  def launch(game: RpgGame, interactive: Boolean = false) = {
    if (interactive) {
      if (lwjglApp == null) {
        val conf = new LwjglApplicationConfiguration();
        conf.title = game.project.data.title;
        conf.width = 32 * 20;
        conf.height = 32 * 15;
        conf.forceExit = false;
        lwjglApp = new LwjglApplication(container, conf)
      }
    } else {
      if (headlessApp == null) {
        headlessApp = new HeadlessApplication(container)
      }
      game.renderingOffForTesting = true
    }

    container.replace(game)
  }
}