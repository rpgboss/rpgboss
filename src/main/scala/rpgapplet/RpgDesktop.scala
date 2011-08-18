package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._


object RpgDesktop extends SwingApplication {
  def top(auth: Option[(String, Long)]) = new MainFrame {
    minimumSize = new Dimension(800, 600)
    title = "rpgboss beta"
    contents = new MainPanel(this)
  }
  
  override def startup(args: Array[String]) = {
    val auth = try {
      if(args.length == 2) Some(args(0)->args(1).toLong) else None
    } catch {
      case e: NumberFormatException => None
    }
    
    // code adapted from SimpleSwingApplication.scala
    val t = top(auth)
    if(t.size == new Dimension(0,0)) t.pack()
    t.visible = true
  }
}

