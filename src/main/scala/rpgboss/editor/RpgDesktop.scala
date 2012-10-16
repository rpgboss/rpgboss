package rpgboss.editor

import scala.swing._
import scala.swing.event._
import com.weiglewilczek.slf4s.Logging
import java.lang.Thread.UncaughtExceptionHandler

object RpgDesktop 
  extends SwingApplication 
  with Logging 
  with UncaughtExceptionHandler {
  
  def top() = new MainFrame {
    minimumSize = new Dimension(800, 600)
    title = "rpgboss beta"
    contents = new MainPanel(this)
  }
 
  def uncaughtException(thread: Thread, ex: Throwable) = {
    logger.error("Uncaught exception")
    ex.printStackTrace()
  }
  
  def handle(ex: Throwable) = {
    uncaughtException(Thread.currentThread(), ex)
  }
  
  override def startup(args: Array[String]) = {
    System.setProperty("sun.awt.exception.handler", getClass().getName());
    Thread.setDefaultUncaughtExceptionHandler(this);
    
    // code adapted from SimpleSwingApplication.scala
    val t = top()
    if(t.size == new Dimension(0,0)) t.pack()
    t.visible = true
  }
}

