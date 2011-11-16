package rpgboss.editor

import scala.swing._

import rpgboss.model._
import rpgboss.message._
import rpgboss.editor.dialog._

class MainPanel(val topWin: Window)
extends BoxPanel(Orientation.Vertical) 
{
  minimumSize = new Dimension(800, 600)
  
  val actionNew = Action("New Project") {
    if(smOpt.map(_.askSaveUnchanged(this)).getOrElse(true)) {
      val d = new NewProjectDialog(topWin, p => setProject(p))
      d.open()
    }
  }
  
  val actionOpen = Action("Load Project") {
    if(smOpt.map(_.askSaveUnchanged(this)).getOrElse(true)) {
      val d = new LoadProjectDialog(topWin, p => setProject(p))
      d.open()
    }
  }
  
  val actionSave = Action("Save Project") {
    smOpt.map(_.save())
  }
  
  def setContent(c: Component) = {
    contents.clear()
    contents += c
    revalidate()
  }
  
  setContent(new StartPanel(this))
  
  var smOpt : Option[StateMaster] = None
  
  def setProject(p: Project) = {
    val sm = new StateMaster(p)
    smOpt = Some(sm)
    setContent(new ProjectPanel(this, sm))
  }
  
  def error(s: String) = {
    println("Error: " + s)
    //setContent(new Label(s))
  }
}
