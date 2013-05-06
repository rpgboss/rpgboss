package rpgboss.editor.uibase

import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas
import com.badlogic.gdx._
import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.Logging

class GdxPanel extends Component with Logging {
  override lazy val peer = new javax.swing.JComponent with SuperMixin {
    override def addNotify() = {
      logger.debug("GdxPanel addNotify")
      gdxCanvas.getCanvas().setSize(10, 10)
      add(gdxCanvas.getCanvas())
    }
    
    override def removeNotify() = {
      logger.debug("GdxPanel removeNotify")
      gdxCanvas.stop()
      cleanup()
    }
  }
  
  def cleanup() = {
    
  }
  
  val gdxListener = new ApplicationAdapter with Logging {
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
      logger.debug("render()")
    }
    override def resize(w: Int, h: Int) = {
      logger.debug("resize(%d, %d)".format(w, h))
    }
    override def resume() = {
      logger.debug("resume()")
    }
  }
  
  val gdxCanvas = new LwjglAWTCanvas(gdxListener, true)
  
  def getAudio() = gdxCanvas.getAudio()
}