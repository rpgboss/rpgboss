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
import javax.swing.JPanel

class GdxPanel(project: Project, canvasW: Int = 10, canvasH: Int = 10)
  extends Panel
  with LazyLogging
  with Disposable {

  val jPanel = new JPanel
  peer.add(jPanel)

  val panel = new java.awt.Panel
  jPanel.add(panel)

  panel.add(gdxCanvas.getCanvas())

  preferredSize = new Dimension(canvasW, canvasH)

  // lazy val, otherwise an NPE crash due to wonky order of initialization
  lazy val gdxListener = new ApplicationAdapter {
    override def create() = {
      logger.debug("Create")
    }
    override def dispose() = {
      logger.debug("Dispose")
    }
    override def pause() = {
      logger.debug("Pause")
    }
    override def render() = {
      //logger.debug("render() %d".format(this.hashCode()))
      //logger.debug("gdx audio hash code %d".format(Gdx.audio.hashCode()))
    }
    override def resize(w: Int, h: Int) = {
      logger.debug("Resize" + "(%d, %d)".format(w, h))
    }
    override def resume() = {
      logger.debug("Resume")
    }
  }

  lazy val gdxCanvas = new LwjglAWTCanvas(gdxListener) {

    logger.info("Gdx Canvas Constructor")

    override def start() = {
      logger.debug("Start")
      super.start()
    }

    override def resize(w: Int, h: Int) = {
      logger.debug("Resize" + "(%d, %d)".format(w, h))
      super.resize(w, h)
    }

    override def stopped() = {
      logger.debug("Stopped")
      super.stopped()
    }

    getCanvas().setSize(canvasW, canvasH)

    override def makeCurrent() = {
      super.makeCurrent()
      logger.debug("MakeCurrent")
    }

    override def stop() = {
      // This is necessary because libgdx currently has a bug where it cannot
      // dispose of textures associated with this canvas on disposal time.
      if (getCanvas().isDisplayable()) {
        makeCurrent()
      } else {
        throw new RuntimeException(
          "GdxPanel OpenGL Context Destroyed Before ApplicationListener" +
            "Had A Chance To Dispose Of Its Resources")
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