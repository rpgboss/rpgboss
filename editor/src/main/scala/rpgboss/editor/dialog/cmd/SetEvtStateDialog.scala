package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.model.RpgEnum
import rpgboss.model.WhichEntity
import rpgboss.model.EntitySpec
import rpgboss.lib.Utils
import rpgboss.editor.StateMaster
import rpgboss.editor.Internationalized._

class SetEventStateDialog(
  owner: Window,
  sm: StateMaster,
  mapName: Option[String],
  initial: SetEventState,
  successF: (SetEventState) => Any)
  extends StdDialog(owner, getMessage("Set_Event_State")) {

  val model = Utils.deepCopy(initial)

  centerDialog(new Dimension(300, 100))

  val fieldWhichEvent = new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = false, allowEventOnOtherMap = true)
  val fieldNewState = new NumberSpinner(model.state, 0, 127, model.state = _)

  contents = new BoxPanel(Orientation.Vertical) {
    contents += fieldWhichEvent
    contents += new DesignGridPanel {
      row().grid(lbl(getMessage("New_State') + ":")).add(fieldNewState)
      addButtons(okBtn, cancelBtn)
    }
  }

  def okFunc() = {
    successF(model)
    close()
  }
}