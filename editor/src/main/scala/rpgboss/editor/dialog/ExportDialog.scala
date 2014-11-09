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

import net.coobird.thumbnailator._

class ExportDialog(
  owner: Window,
  sm: StateMaster,
  mainp: MainPanel)
  extends StdDialog(owner, "Export Project") {

  centerDialog(new Dimension(600, 300))
  resizable = false

  var exportType = 0

  def ExportDesktop() {
      if (sm.askSaveUnchanged(mainp)) {
        val jarFile: File = {
          val envVarValue = System.getenv("RPGBOSS_EXPORT_JAR_PATH")
          if (envVarValue != null) {
            new File(envVarValue)
          } else {
            val classLoc =
              getClass.getProtectionDomain().getCodeSource().getLocation()
            new File(classLoc.getPath())
          }
        }

        if (jarFile.isFile()) {
          Export.export(sm.getProj, jarFile)
          val exportedDir = new File(sm.getProj.dir, "export")
          Dialog.showMessage(
              mainp,
              "Export complete. Packages in: \n"+
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
  }

  def choosePlainFile(title: String = "", txtfield: TextField, lable:Label): Option[File] = {  
    val chooser = new FileChooser(new File("."))
    chooser.title = title
    val result = chooser.showOpenDialog(null)
    if (result == FileChooser.Result.Approve) {
      txtfield.text = chooser.selectedFile.getCanonicalPath()
      lable.icon = new ImageIcon(txtfield.text)
      Some(chooser.selectedFile)
    } else None
  }

  def okFunc() {

  }

  def test() {
    Thumbnails.of("original.jpg")
            .size(160, 160)
            .toFile("thumbnail.jpg");
  }

  contents = new BoxPanel(Orientation.Vertical) {

    contents += new Label("Game Executable Icon")

    val img = new Label { 
      icon = new ImageIcon(rpgboss.lib.Utils.readClasspathImage("hendrik-weiler-theme/exporticon.jpg")) 
    }

    contents += new BoxPanel(Orientation.Horizontal) {
      val selectImgBtn = new Button("Choose icon")
      val selectImgTextField = new TextField("")
      listenTo(selectImgBtn)
      reactions += {
        case ButtonClicked(this.selectImgBtn) => 
          choosePlainFile("Choose your Icon", selectImgTextField, img)
      }
      contents += selectImgTextField
      contents += selectImgBtn
    }

    contents += img

    contents += new Label("Export type")

    val dirFileSelector = List(
        new RadioButton() {
          name = "desktop"
          text = "Windows/Osx/Linux"
          selected = true
        },
        new RadioButton() {
          name = "web"
          text = "WebPlayer"
        },
        new RadioButton() {
          name = "android"
          text = "Android"
        }
      )

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

    contents += new BoxPanel(Orientation.Horizontal) {
      
      val exportBtn = new Button("Export Project")
      val closeBtn = new Button("Cancel")
      listenTo(exportBtn)
      listenTo(closeBtn)
      reactions += {
        case ButtonClicked(this.exportBtn) => 
          if(exportType==0) {
            ExportDesktop()
          }
          if(exportType==1 || exportType==2) {
            Dialog.showMessage(
                mainp,
                "These Options are not implemented yet.",
                "Not available",
                Dialog.Message.Error)
          }
        case ButtonClicked(this.closeBtn) => 
          dispose()
      }

      contents += exportBtn
      contents += closeBtn

    }
  }

}