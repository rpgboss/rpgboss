package rpgboss.rpgapplet.dialog

import rpgboss.rpgapplet.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.model._

import rpgboss.message.Messages._

import net.java.dev.designgridlayout._

class NewProjectDialog(owner: Window, onSuccess: Project => Any) 
  extends StdDialog(owner, "New Project")
{  
  def label(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }
  
  val projectsRootField = new TextField() {
    columns = 18
  }
  
  val shortnameField = new TextField() {
    columns = 12
  }
  
  val gameTitleField = new TextField() {
    columns = 20
  }
  
  val statusLabel = new Label(" ")
  
  val rootChooser = new FileChooser() {
    fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    multiSelectionEnabled = false
    title = "Choose directory to contain all projects"
  }
  
  def okFunc() = {
  }
  
  val elipsisBtn = new Button(Action("...") {
  })
  
  contents = new DesignGridPanel {
    
    row().grid().add(label("Directory for all projects:"))
    row().grid().add(projectsRootField, 6).add(elipsisBtn)
    
    row().grid().add(label("Project shortname:"))
    row().grid().add(shortnameField)
    
    row().grid().add(label("Game title:"))
    row().grid().add(gameTitleField)
    
    row().grid().add(statusLabel)
    
    addButtons(cancelButton, okButton)
  }
}