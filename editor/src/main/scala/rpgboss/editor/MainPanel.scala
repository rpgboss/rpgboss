package rpgboss.editor

import scala.swing._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.dialog._
import java.io.File

class MainPanel(val topWin: MainFrame)
extends BoxPanel(Orientation.Vertical) 
{
  var smOpt : Option[StateMaster] = None
  
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
  
  Settings.get("project.last") map { path =>
    val file = new File(path)
    if(file.isDirectory() && file.canRead()) {
      Project.readFromDisk(file) map { proj =>
        setProject(proj)
      }
    }
  }
  
  def setProject(p: Project) = {
    val sm = new StateMaster(this, p)
    smOpt = Some(sm)
    setContent(new ProjectPanel(this, sm))
    updateDirty(sm)
  }
  
  def updateDirty(sm: StateMaster) = {
    if(sm.stateDirty) {
      topWin.title = "rpgboss beta - %s*".format(sm.getProj.data.title)
    } else {
      topWin.title = "rpgboss beta - %s".format(sm.getProj.data.title)
    }
  }
  
  def error(s: String) = {
    println("Error: " + s)
    //setContent(new Label(s))
  }
}
