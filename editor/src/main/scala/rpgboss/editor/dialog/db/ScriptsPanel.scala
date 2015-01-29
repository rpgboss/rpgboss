package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.Internationalized._

class ScriptsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel {
    def panelName = getMessage("Scripts")

    var project = sm.getProj

    var upButton = new CheckBox("Up")
    var leftButton = new CheckBox("Left")
    var rightButton = new CheckBox("Right")
    var downButton = new CheckBox("Down")
    var enterButton = new CheckBox("Enter")
    var escapeButton = new CheckBox("Escape")

    def enableCheckboxes(result: Boolean) = {
      upButton.enabled = result
      leftButton.enabled = result
      rightButton.enabled = result
      downButton.enabled = result
      enterButton.enabled = result
      escapeButton.enabled = result
    }
    enableCheckboxes(false)

    listenTo(upButton, leftButton, rightButton, downButton)
    reactions += {
      case ButtonClicked(upButton) => 
        println(myListView.getSelection)
      case ButtonClicked(leftButton) => 
        println("klicked")
      case ButtonClicked(rightButton) => 
        println("klicked")
      case ButtonClicked(downButton) => 
        println("klicked")
    }

    var keysPanel = new BoxPanel(Orientation.Vertical) {

      contents += new Label("Key Mapping")
      contents += Swing.VStrut(5)

      contents += upButton
      contents += leftButton
      contents += rightButton
      contents += downButton
      contents += enterButton
      contents += escapeButton
    }

    var myListView = new ScriptsListView(project) {
      override def onScriptSelected(selection:String):Unit = {
        enableCheckboxes(true)
        println(selection)
      }
    }

    row.grid().add(myListView).add(keysPanel)

}