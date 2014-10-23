package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.model.RpgEnum

class SetEventStateDialog(
  owner: Window,
  initial: SetEventState,
  successF: (SetEventState) => Any)
  extends StdDialog(owner, "Set Event State") {
  
  val fieldNewState = new NumberSpinner(initial.state, 0, 127)
  
  contents = new DesignGridPanel {
    row().grid(lbl("New state:")).add(fieldNewState)
    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    successF(SetEventState(fieldNewState.getValue))
    close()
  }
}