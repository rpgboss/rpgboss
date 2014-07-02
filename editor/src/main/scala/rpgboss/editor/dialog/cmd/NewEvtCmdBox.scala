package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.editor.dialog._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog.cmd._
import rpgboss.model.event._
import rpgboss.model.Constants._
import rpgboss.model._
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.dialog.EventDialog

class NewEvtCmdBox(
  evtDiag: EventDialog,
  sm: StateMaster,
  owner: Window,
  mapName: String,
  cmdBox: CommandBox,
  idxToInsert: Int)
  extends StdDialog(owner, "New command") {

  // Noop, as there is no okay button
  def okFunc() = {}

  def btnEvtCmd(title: String, e: EventCmd) = {
    new Button() {
      action = Action(title) {
        val d = EventCmdDialog.dialogFor(
          owner,
          sm,
          mapName,
          e,
          evtCmd => {
            NewEvtCmdBox.this.close()
            cmdBox.insertCmd(idxToInsert, evtCmd)
          })

        if (d != null) {
          d.open()
        } else {
          cmdBox.insertCmd(idxToInsert, e)
          NewEvtCmdBox.this.close()
        }
      }
    }
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Windows:"))
    row().grid().add(btnEvtCmd("Show text...", ShowText()))

    row().grid().add(leftLabel("Movement:"))
    row().grid().add(btnEvtCmd("Teleport player...",
      Teleport(
        MapLoc(evtDiag.mapName, evtDiag.event.x, evtDiag.event.y),
        Transitions.FADE.id)))
    row().grid().add(btnEvtCmd("Move event...", MoveEvent()))
    row().grid().add(btnEvtCmd("Lock player movement...",
      LockPlayerMovement(Array())))

    row().grid().add(leftLabel("Party:"))
    row().grid().add(btnEvtCmd("Modify Party...", ModifyParty()))

    row().grid().add(leftLabel("Other:"))
    row().grid().add(btnEvtCmd("Change event state...", SetEvtState()))
    row().grid().add(btnEvtCmd("Start battle...", StartBattle()))
    row().grid().add(btnEvtCmd("Run Javascript...", RunJs()))

    addCancel(cancelBtn)
  }

}