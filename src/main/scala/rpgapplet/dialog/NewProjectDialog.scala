package rpgboss.rpgapplet.dialog

import rpgboss.rpgapplet.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.model._

import rpgboss.message.Messages._

import net.java.dev.designgridlayout._

import java.io.File

class NewProjectDialog(owner: Window, onSuccess: Project => Any) 
  extends StdDialog(owner, "New Project")
{  
  def label(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }
  
  val rootChooser = new FileChooser() {
    fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    multiSelectionEnabled = false
    title = "Choose directory to contain all projects"
  }
  
  val projectsRootField = new TextField() {
    columns = 18
    text = rootChooser.peer.getFileSystemView.getDefaultDirectory.getPath
  }
  
  val shortnameField = new TextField() {
    columns = 12
  }
  
  val gameTitleField = new TextField() {
    columns = 20
  }
  
  val statusLabel = new Label(" ")
  
  def okFunc() = {
    if(shortnameField.text.isEmpty)
      Dialog.showMessage(shortnameField, "Need a short name.")
    else {    
      val p = Project.startingProject(shortnameField.text, 
                                      gameTitleField.text,
                                      projectsRootField.text)
      if(p.writeToDisk())
        onSuccess(p)
      else 
        Dialog.showMessage(okButton, "Could not write file", "Error", 
                           Dialog.Message.Error)
    }
  }
  
  val elipsisBtn = new Button(Action("...") {
    if(rootChooser.showDialog(projectsRootField, "Select") == 
       FileChooser.Result.Approve)
      projectsRootField.text = rootChooser.selectedFile.getPath
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
    
    shortnameField.requestFocus()
  }
}