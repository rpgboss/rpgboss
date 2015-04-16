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
  val Windows = Value
  val Input = Value
  val Movement = Value
  val Party = Value
  val Inventory = Value
  val Battles = Value
  val Effects = Value
  val Programming = Value
  val GameState = Value
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
        val dOpt = EventCmdDialog.dialogFor(
          owner,
          sm,
          eventLoc.map(_.map),
          ui.newInstance(eventLoc),
          evtCmd => {
            NewEventCmdBox.this.close()
            cmdBox.insertCmd(idxToInsert, evtCmd)
          })

        dOpt.map { d =>
          d.open()
        } getOrElse {
          cmdBox.insertCmd(idxToInsert, ui.newInstance(eventLoc))
          NewEventCmdBox.this.close()
        }
      })
    }
  }

  val columns = {
    import EventCmdCategory._
    Array(
      Array(Windows, Input),
      Array(Movement, Party, Inventory),
      Array(Battles, Effects, GameState),
      Array(Programming)
    )
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {

      for (categories <- columns) {
        contents += new DesignGridPanel {
          for (category <- categories) {
            val cmdUisInCategory =
              EventCmdUI.eventCmdUis.filter(_.category == category)

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