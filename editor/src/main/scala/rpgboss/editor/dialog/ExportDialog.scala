package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.uibase._
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page
import rpgboss.model._
import rpgboss.editor.StateMaster
import java.awt.Dimension
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.resourceselector.SpriteField
import javax.swing.BorderFactory
import rpgboss.lib.Utils
import rpgboss.editor.MainPanel
import java.io._
import java.awt.Desktop
import rpgboss.editor.util.Export
import javax.swing.ImageIcon
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }
import net.coobird.thumbnailator._
import org.apache.commons.io.FileUtils

class PleaseWaitFrame extends Frame {
  title = "Please Wait"
  minimumSize = new Dimension(320, 240)
  centerOnScreen()
  contents = new Label("Now exporting...")
}

class ExportDialog(
  owner: Window,
  sm: StateMaster,
  mainp: MainPanel)
  extends StdDialog(owner, "Export Project") {

  var exportType = 0

  def exportDesktop(): Int = {
    if (sm.askSaveUnchanged(mainp)) {
      val jarFile: File = {
        var envVarValue = System.getenv("RPGBOSS_EXPORT_JAR_PATH")
        if (envVarValue != null) {
          new File(envVarValue)
        } else {
          val classLoc =
            getClass.getProtectionDomain().getCodeSource().getLocation()
          FileUtils.toFile(classLoc)
        }
      }

      if (jarFile.isFile()) {
        Export.export(sm.getProj, jarFile)
        val exportedDir = new File(sm.getProj.dir, "export")
        Dialog.showMessage(
          mainp,
          "Export complete. Packages in: \n" +
            exportedDir.getCanonicalPath(),
          "Export Complete",
          Dialog.Message.Info)
        if (Desktop.isDesktopSupported())
          Desktop.getDesktop().browse(exportedDir.toURI)
      } else {
        Dialog.showMessage(
          mainp,
          "Cannot locate rpgboss JAR for export. Path tried: \n" +
            jarFile.getCanonicalPath() + "\n\n" +
            "If you are running from an IDE or SBT for development,\n" +
            "set the RPGBOSS_EXPORT_JAR_PATH environment variable.",
          "Cannot export",
          Dialog.Message.Error)
      }
    }
    return 1;
  }

  def okFunc() {
    if (exportType == 0) {
      val waitFrame = new PleaseWaitFrame
      waitFrame.open()
      close()
      val f: Future[Int] = Future {
        waitFrame.visible = true
        exportDesktop()
      }
      f onSuccess {
        case _ =>
          waitFrame.close()
          waitFrame.dispose()
      }
    } else {
      Dialog.showMessage(
        mainp,
        "These Options are not implemented yet.",
        "Not available",
        Dialog.Message.Error)
    }
  }

  okBtn.text = "Export"

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new Label("Export type")

    val dirFileSelector = List(
      new RadioButton() {
        name = "desktop"
        text = "Windows/Osx/Linux"
        selected = true
      })

    new ButtonGroup(dirFileSelector: _*)

    contents ++= dirFileSelector
    dirFileSelector.foreach(listenTo(_))
    reactions += {
      case ButtonClicked(button) => {
        button.name match {
          case "desktop" =>
            exportType = 0
          case "web" =>
            exportType = 1
          case "android" =>
            exportType = 2
        }
      }
    }

    contents += new DesignGridPanel {
      addButtons(okBtn, cancelBtn)
    }
  }

}