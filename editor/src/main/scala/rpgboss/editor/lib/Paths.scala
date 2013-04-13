package rpgboss.editor.lib

import scala.swing._
import scala.swing.event._
import rpgboss.editor._

object Paths {
  def getRootChooserPanel(changedCallback: () => Unit) =
    new BoxPanel(Orientation.Horizontal) {

      val rootChooser = new FileChooser() {
        fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
        multiSelectionEnabled = false
        title = "Choose directory to contain all projects"
      }

      val projectsRootField = new TextField() {
        columns = 18
        text = Settings.get("project.directory") getOrElse
          rootChooser.peer.getFileSystemView.getDefaultDirectory.getPath
        editable = false
        enabled = true
      }

      def showDirBrowser() = {
        if (rootChooser.showDialog(projectsRootField, "Select") ==
          FileChooser.Result.Approve) {
          val newPath = rootChooser.selectedFile.getPath
          projectsRootField.text = newPath
          Settings.set("project.directory", newPath)
          changedCallback()
        }
      }

      val elipsisBtn = new Button(Action("...")(showDirBrowser))

      contents += projectsRootField
      contents += elipsisBtn

      def getRoot = new java.io.File(projectsRootField.text)

      listenTo(projectsRootField.mouse.clicks)

      reactions += {
        case MouseClicked(`projectsRootField`, _, _, 2, _) =>
          showDirBrowser()
      }
    }
}
