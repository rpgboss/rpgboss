package rpgboss.editor.dialog.cmd

import scala.swing.Action
import scala.swing.BoxPanel
import scala.swing.Button
import scala.swing.Dimension
import scala.swing.Orientation
import scala.swing.Window

import rpgboss.editor.Internationalized.getMessage
import rpgboss.editor.Internationalized.getMessageColon
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.CommandBox
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils.leftLabel
import rpgboss.model.MapLoc

object EventCmdCategory extends Enumeration {
  val Windows, Movement, Party, Inventory = Value
  val Battles, Audio, Programming = Value
}

class NewEventCmdBox(
  sm: StateMaster,
  owner: Window,
  eventLoc: Option[MapLoc],
  cmdBox: CommandBox,
  idxToInsert: Int)
  extends StdDialog(owner, getMessage("New_Command")) {

  centerDialog(new Dimension(400, 400))

  // Noop, as there is no okay button
  def okFunc() = {}

  def btnEvtCmd(title: String, ui: EventCmdUI[_], eventLoc: Option[MapLoc]) = {
    new Button() {
      action = Action(title) ({
        val d = EventCmdDialog.dialogFor(
          owner,
          sm,
          eventLoc.map(_.map),
          ui.newInstance(eventLoc),
          evtCmd => {
            NewEventCmdBox.this.close()
            cmdBox.insertCmd(idxToInsert, evtCmd)
          })

        if (d != null) {
          d.open()
        } else {
          cmdBox.insertCmd(idxToInsert, ui.newInstance(eventLoc))
          NewEventCmdBox.this.close()
        }
      })
    }
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      val categoriesPerColumn = 4
      val nCategories = EventCmdCategory.values.size
      val columns = (nCategories / categoriesPerColumn) + 1

      for (columnId <- 0 until columns) {
        contents += new DesignGridPanel {
          val startI = columnId * categoriesPerColumn
          for (i <- startI until startI + 4; if i < nCategories) {
            val category = EventCmdCategory(i)
            val cmdUisInCategory =
              EventCmdDialog.eventCmdUis.filter(_.category == category)

            row().grid().add(leftLabel(getMessageColon(category.toString)))
            for (cmdUi <- cmdUisInCategory) {
              row().grid().add(btnEvtCmd(cmdUi.title, cmdUi, eventLoc))
            }
          }
        }
      }
    }

    contents += new DesignGridPanel {
      addCancel(cancelBtn)
    }
  }

}