package rpgboss.editor

import scala.swing._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.dialog._
import java.io.File
import rpgboss.editor.Internationalized._ 

class MainPanel(val topWin: Frame)
  extends BoxPanel(Orientation.Vertical) {
  var smOpt: Option[StateMaster] = None

  minimumSize = new Dimension(800, 600)

  def getWindow() = {
    topWin
  }
  val window = topWin

  val actionNew = Action(getMessage("New_Project")) {
    if (askSaveUnchanged()) {
      val d = new NewProjectDialog(topWin, p => setProject(p))
      d.open()
    }
  }

  val actionOpen = Action(getMessage("Load_Project")) {
    if (askSaveUnchanged()) {
      val d = new LoadProjectDialog(topWin, p => setProject(p))
      d.open()
    }
  }

  def askSaveUnchanged() = {
    smOpt.map(_.askSaveUnchanged(this)).getOrElse(true)
  }

  val actionSave = Action(getMessage("Save_Project")) {
    smOpt.map(_.save())
  }

  def setContent(c: Component) = {
    contents.clear()
    contents += c
    revalidate()
  }

  val actionClose = Action(getMessage("Close_Project")) {
    Settings.set("project.last","");
    setContent(new StartPanel(this))
    window.unmaximize()
    window.minimumSize = new Dimension(600, 400)
    window.size = new Dimension(600, 400)
    window.resizable = false
    window.centerOnScreen()
  }

  setContent(new StartPanel(this))
  
  Settings.get("project.last") map { path =>
    val file = new File(path)
    if (file.isDirectory() && file.canRead()) {
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
    if (sm.stateDirty) {
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
