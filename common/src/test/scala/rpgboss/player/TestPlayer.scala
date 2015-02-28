package rpgboss.player

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import java.nio.IntBuffer
import org.mockito.Matchers

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
        {
          import org.mockito.stubbing.Answer

          val mockGL20 = mock(classOf[GL20])
          Gdx.gl = mockGL20
          Gdx.gl20 = mockGL20
          Gdx.gl30 = mock(classOf[GL30])

          def getFakeGLAnswer(intAnswer: Int) = new Answer[Unit]() {
            override def answer(invocation: InvocationOnMock) = {
              val list = invocation.getArguments
              list(2).asInstanceOf[IntBuffer].put(0, intAnswer)
            }
          }

          when(mockGL20.glCreateShader(anyInt())).thenReturn(5)
          when(mockGL20.glCreateProgram()).thenReturn(5)

          doAnswer(getFakeGLAnswer(5))
            .when(mockGL20).glGetShaderiv(anyInt(), anyInt(), anyObject())
          doAnswer(getFakeGLAnswer(5))
            .when(mockGL20).glGetProgramiv(
                anyInt(), Matchers.eq(GL20.GL_LINK_STATUS), anyObject())
          doAnswer(getFakeGLAnswer(0))
            .when(mockGL20).glGetProgramiv(
                anyInt(), Matchers.eq(GL20.GL_ACTIVE_ATTRIBUTES), anyObject())
        }
      }
    }

    container.replace(game)
  }
}