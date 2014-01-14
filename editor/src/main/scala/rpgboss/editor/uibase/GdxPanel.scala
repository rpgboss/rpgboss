package rpgboss.editor.uibase

import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.Logging
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.backends.openal.OpenALAudio

class GdxPanel(canvasW: Int = 10, canvasH: Int = 10) 
  extends Component 
  with Logging
  with Disposable {
  
  override lazy val peer = new javax.swing.JComponent with SuperMixin {
    add(gdxCanvas.getCanvas())
  }
  
  preferredSize = new Dimension(canvasW, canvasH)
  
  val gdxListener = new ApplicationAdapter {
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
  
  lazy val gdxCanvas = new LwjglAWTCanvas(gdxListener, false) {
    
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
      logger.debug("stop()")
      super.stop()
    }
  }
  
  def dispose() = {
    logger.debug("Destroying GdxPanel")
    gdxCanvas.stop()
    if (Gdx.audio != null) {
      Gdx.audio.asInstanceOf[OpenALAudio].dispose()
      Gdx.audio = null
      logger.debug("Destroying gdx audio from GdxPanel")
    }
  }
  
  def getAudio() = gdxCanvas.getAudio()
}