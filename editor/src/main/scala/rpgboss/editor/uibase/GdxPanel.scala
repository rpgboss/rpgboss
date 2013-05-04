package rpgboss.editor.uibase

import com.badlogic.gdx.backends.lwjgl.LwjglCanvas
import com.badlogic.gdx._
import scala.swing._
import com.typesafe.scalalogging.slf4j.Logging

class GdxPanel extends Component with Logging {
  override lazy val peer = new javax.swing.JPanel with SuperMixin
  
  val gdxListener = new ApplicationAdapter {
    
  }
  
  private val gdxCanvas = new LwjglCanvas(gdxListener, false) {
    override def start() = {
      logger.debug("LwjglCanvas start")
      super.start()
    }
    
    override def stop() = {
      logger.debug("LwjglCanvas stop")
      super.stop()
    }  
  }
  
  def getAudio() = gdxCanvas.getAudio()
  
  peer.add(gdxCanvas.getCanvas())
}