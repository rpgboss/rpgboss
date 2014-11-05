package rpgboss.editor.dialog

import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.misc._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import java.io._
import rpgboss.lib.FileHelper._
import rpgboss.editor.uibase.StdDialog
import rpgboss.model.resource.Resource

class NewProjectDialog(owner: Window, onSuccess: Project => Any)
  extends StdDialog(owner, "New Project") {
  val rootChooser = Paths.getRootChooserPanel(() => Unit)

  centerDialog(new Dimension(400, 200))

  val shortnameField = new TextField() {
    columns = 12
  }

  def okFunc() = {
    if (shortnameField.text.isEmpty)
      Dialog.showMessage(shortnameField, "Need a short name.")
    else {
      val shortname = shortnameField.text
      val projectDirectory = new File(rootChooser.getRoot, shortname)

      val projectOption =
        rpgboss.util.ProjectCreator.create(shortname, projectDirectory)
      
      if (projectOption.isDefined) {
        onSuccess(projectOption.get)
        close()
      } else
        Dialog.showMessage(okBtn, "File write error", "Error",
          Dialog.Message.Error)
    }
  }

  contents = new DesignGridPanel {

    row().grid().add(leftLabel("Directory for all projects:"))
    row().grid().add(rootChooser)

    row().grid().add(leftLabel("Project shortname (ex. 'chronotrigger'):"))
    row().grid().add(shortnameField)

    addButtons(cancelBtn, okBtn)

    shortnameField.requestFocus()
  }
}