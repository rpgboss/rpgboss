package rpgboss.editor

import scala.swing._

import rpgboss.model._
import rpgboss.message._

class MainPanel(val topWin: Window)
extends BoxPanel(Orientation.Vertical) 
{
  minimumSize = new Dimension(800, 600)
  
  def setContent(c: Component) = {
    contents.clear()
    contents += c
    revalidate()
  }
  
  setContent(new StartPanel(this))
  
  def projectGui(p: Project) = {
    setContent(new ProjectPanel(this, p))
  }
  
  def error(s: String) = {
    println("Error: " + s)
    //setContent(new Label(s))
  }
}
