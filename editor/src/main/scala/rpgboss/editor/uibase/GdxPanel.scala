package rpgboss.editor.uibase

import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio
import rpgboss.model.resource.RpgAssetManager
import rpgboss.model.Project
import rpgboss.editor.Internationalized._

class GdxPanel(project: Project, canvasW: Int = 10, canvasH: Int = 10)
  extends Component
  with LazyLogging
  with Disposable {

  override lazy val peer = new javax.swing.JComponent with SuperMixin {
    add(gdxCanvas.getCanvas())
  }

  preferredSize = new Dimension(canvasW, canvasH)

  // lazy val, otherwise an NPE crash due to wonky order of initialization
  lazy val gdxListener = new ApplicationAdapter {
    override def create() = {
      logger.debug("create()")
    }
    override def dispose() = {
      logger.debug("dispose()")
    }
    override def pause() = {
      logger.debug("pause()")
    }
    override def render() = {
      //logger.debug("render() %d".format(this.hashCode()))
      //logger.debug("gdx audio hash code %d".format(Gdx.audio.hashCode()))
    }
    override def resize(w: Int, h: Int) = {
      logger.debug("resize(%d, %d)".format(w, h))
    }
    override def resume() = {
      logger.debug("resume()")
    }
  }

  lazy val gdxCanvas = new LwjglAWTCanvas(gdxListener) {

    logger.info("Gdx Canvas constructor")

    override def start() = {
      logger.debug("start()")
      super.start()
    }

    override def resize(w: Int, h: Int) = {
      logger.debug("resize(%d, %d)".format(w, h))
      super.resize(w, h)
    }

    override def stopped() = {
      logger.debug("stopped()")
      super.stopped()
    }

    getCanvas().setSize(canvasW, canvasH)

    override def makeCurrent() = {
      super.makeCurrent()
      logger.debug("makeCurrent()")
    }

    override def stop() = {
      // This is necessary because libgdx currently has a bug where it cannot
      // dispose of textures associated with this canvas on disposal time.
      if (getCanvas().isDisplayable()) {
        makeCurrent()
      } else {
        throw new RuntimeException(
          "GdxPanel's OpenGL context destroyed before ApplicationListener " +
            "had a chance to dispose of its resources.")
      }

      super.stop()
    }
  }

  def dispose() = {
    logger.debug("Destroying GdxPanel")
    gdxCanvas.stop()
  }

  def getAudio() = gdxCanvas.getAudio()
}