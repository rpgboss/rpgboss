package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.model.RpgEnum
import rpgboss.model.WhichEntity
import rpgboss.model.EntitySpec

class SetEventStateDialog(
  owner: Window,
  initial: SetEventState,
  successF: (SetEventState) => Any)
  extends StdDialog(owner, "Set Event State") {

  centerDialog(new Dimension(300, 100))

  val fieldNewState = new NumberSpinner(initial.state, 0, 127)

  contents = new DesignGridPanel {
    row().grid(lbl("New state:")).add(fieldNewState)
    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    successF(SetEventState(EntitySpec(WhichEntity.THIS_EVENT.id),
        fieldNewState.getValue))
    close()
  }
}