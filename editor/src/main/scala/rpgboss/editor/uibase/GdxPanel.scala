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
    }
    
    override def removeNotify() = {
      logger.debug("GdxPanel removeNotify")
      gdxCanvas.stop()
      cleanup()
    }
  }
  
  def cleanup() = {
    
  }
  
  val gdxListener = new ApplicationAdapter {
    
  }
  
  private val gdxCanvas = new LwjglAWTCanvas(gdxListener, false)
  
  def getAudio() = gdxCanvas.getAudio()
  
  peer.add(gdxCanvas.getCanvas())
}