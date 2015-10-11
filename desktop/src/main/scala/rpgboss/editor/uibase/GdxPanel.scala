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

/**
 * @param   canvasW     Very small canvases (like 10 px), don't seem to be
 *                      displayed and initialized properly. This is why the
 *                      default size is 40.
 */
class GdxPanel(project: Project, canvasW: Int = 40, canvasH: Int = 40)
  extends Panel
  with LazyLogging
  with Disposable {

  val canvasSize = new Dimension(canvasW, canvasH)
  val jPanel = new JPanel
  peer.add(jPanel)

  val panel = new java.awt.Panel
  jPanel.add(panel)

  panel.add(gdxCanvas.getCanvas())

  preferredSize = canvasSize

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
    }
    override def resize(w: Int, h: Int) = {
      logger.debug("Resize" + "(%d, %d)".format(w, h))
    }
    override def resume() = {
      logger.debug("Resume")
    }
  }

  lazy val gdxCanvas = new LwjglAWTCanvas(gdxListener) {
    override def start() = {
      logger.debug("Start")
      super.start()
    }

    override def resize(w: Int, h: Int) = {
      logger.debug("Resize" + "(%d, %d)".format(w, h))
      super.resize(w, h)
    }

    getCanvas().setSize(canvasW, canvasH)
  }

  def dispose() = {
    logger.debug("Destroying GdxPanel")
    gdxCanvas.stop()
  }

  def getAudio() = gdxCanvas.getAudio()
}
