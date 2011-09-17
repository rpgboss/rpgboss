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
  val rootChooser = Paths.getRootChooserPanel(() => Unit)
  
  val shortnameField = new TextField() {
    columns = 12
  }
  
  val gameTitleField = new TextField() {
    columns = 20
  }
  
  def okFunc() = {
    if(shortnameField.text.isEmpty)
      Dialog.showMessage(shortnameField, "Need a short name.")
    else {    
      val shortname = shortnameField.text
      val p = Project.startingProject(shortname, 
                                      gameTitleField.text,
                                      new File(rootChooser.getRoot, shortname))
      
      val m = RpgMap.defaultMap
      
      val allSavedOkay = 
        p.writeMetadata() &&
        m.saveMetadata(p) &&
        m.saveMapData(p, RpgMap.defaultMapData)
      
      if(allSavedOkay) {
        onSuccess(p)
        close()
      }
      else 
        Dialog.showMessage(okButton, "File write error", "Error", 
                           Dialog.Message.Error)
    }
  }
  
  
  contents = new DesignGridPanel {
    
    row().grid().add(leftLabel("Directory for all projects:"))
    row().grid().add(rootChooser)
    
    row().grid().add(leftLabel("Project shortname (ex. 'chronotrigger'):"))
    row().grid().add(shortnameField)
    
    row().grid().add(leftLabel("Game title (ex: 'Chrono Trigger'):"))
    row().grid().add(gameTitleField)
    
    addButtons(cancelButton, okButton)
    
    shortnameField.requestFocus()
  }
}