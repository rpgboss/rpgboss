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
import rpgboss.editor.Internationalized._

class NewProjectDialog(owner: Window, onSuccess: Project => Any)
  extends StdDialog(owner, getMessage("New_Project")) {
  val rootChooser = Paths.getRootChooserPanel(() => Unit)

  centerDialog(new Dimension(400, 200))

  val shortnameField = new TextField() {
    columns = 12
  }

  def okFunc() = {
    if (shortnameField.text.isEmpty)
      Dialog.showMessage(shortnameField, getMessage("Need_Short_Name"))
    else {
      val shortname = shortnameField.text
      val projectDirectory = new File(rootChooser.getRoot, shortname)

      val projectOption =
        rpgboss.util.ProjectCreator.create(shortname, projectDirectory)
      
      if (projectOption.isDefined) {
        onSuccess(projectOption.get)
        close()
      } else
        Dialog.showMessage(okBtn, getMessage("File_Write_Error"), getMessage("Error"),
          Dialog.Message.Error)
    }
  }

  contents = new DesignGridPanel {

    row().grid().add(leftLabel(getMessage("Directory_Project" + ":")))
    row().grid().add(rootChooser)

    row().grid().add(leftLabel(getMessage("Project_Shortname") + ":"))
    row().grid().add(shortnameField)

    addButtons(okBtn, cancelBtn)

    shortnameField.requestFocus()
  }
}