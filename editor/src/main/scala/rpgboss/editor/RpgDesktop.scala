package rpgboss.editor

import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.lang.Thread.UncaughtExceptionHandler
import javax.swing.UIManager
import javax.imageio.ImageIO
import rpgboss.editor.Internationalized._

object RpgDesktop
  extends SwingApplication
  with LazyLogging
  with UncaughtExceptionHandler {

  def top() = new MainFrame {
    minimumSize = new Dimension(600, 400)
    resizable = false
    centerOnScreen()
    title = getMessage("rpgboss")

    import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
    peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)

    val mainPanel = new MainPanel(this)

    contents = mainPanel

    override def closeOperation() = {
      if (mainPanel.askSaveUnchanged()) {
        super.closeOperation()
      }
      if(VisibleConnection.currentSession!=null) {
        VisibleConnection.currentSession.close()
      }
    }
  }

  def uncaughtException(thread: Thread, ex: Throwable) = {
    logger.error("Uncaught exception")
    ex.printStackTrace()
  }

  def handle(ex: Throwable) = {
    uncaughtException(Thread.currentThread(), ex)
  }

  override def startup(args: Array[String]) = {
    UIManager.setLookAndFeel(
      UIManager.getSystemLookAndFeelClassName());
    
    val icon = rpgboss.lib.Utils.readClasspathImage("icon.png")
    
    System.setProperty("sun.awt.exception.handler", "rpgboss.editor.EDTErrorHandler");
    Thread.setDefaultUncaughtExceptionHandler(this);

    // code adapted from SimpleSwingApplication.scala
    val t = top()
    t.peer.setIconImage(icon)
    if (t.size == new Dimension(0, 0)) t.pack()
    t.visible = true
  }
  
  override def main(args: Array[String]) {
    if (args.size >= 2 && args.head == "--player") {
      rpgboss.player.LwjglPlayer.main(args.tail)
    } else {
      super.main(args)
    }
  }
}

class EDTErrorHandler {
  /**
   * This method is invoked by the AWT event dispatch mechanism when an
   * unexpected exception or error is thrown during event dispatching.
   */
  def handle(t: Throwable) {
    t.printStackTrace()
  }
}

