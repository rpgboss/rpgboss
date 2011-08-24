package rpgboss.rpgapplet

import scala.swing._

import rpgboss.model._
import rpgboss.message._

class MainPanel(val topWin: Window)
extends BoxPanel(Orientation.Vertical) 
{
  def setContent(c: Component) = {
    contents.clear()
    contents += c
    revalidate()
  }
  
  setContent(new StartPanel(this))
  
  def projectGui(p: Project) = {
    
  }
  
  def error(s: String) = {
    println("Error: " + s)
    //setContent(new Label(s))
  }
}
