package rpgboss.rpgapplet

import scala.swing._

import rpgboss.model._
import rpgboss.message._

class MainPanel(val username: String, val token: Long, val toEdit: String)
extends BoxPanel(Orientation.Vertical) 
{
  val head = Header(username, token, ObjName.resolve(toEdit))
  
  def objName = head.name
  
  def setContent(c: Component) = {
    println("setContent")
    contents.clear()
    contents += c
    repaint()
  }
  
  setContent(new LoadingPanel(this))
  
  def handleNoSuchItem() = {
    setContent(new TilesetNewPanel(this))
  }
  
  def error(s: String) = {
    
    
    println("Error: " + s)
    setContent(new Label(s))
  }
}
