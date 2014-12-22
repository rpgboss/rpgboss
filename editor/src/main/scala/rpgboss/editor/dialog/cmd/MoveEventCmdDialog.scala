package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.StateMaster
import rpgboss.lib.Utils
import rpgboss.editor.Internationalized._

class MoveEventCmdDialog(
  owner: Window,
  sm: StateMaster,
  mapName: Option[String],
  initial: MoveEvent,
  successF: (MoveEvent) => Any)
  extends StdDialog(owner, getMessage("Move_Event")) {

  centerDialog(new Dimension(200, 300))

  val model = Utils.deepCopy(initial)

  val fieldWhichEvent = new EntitySelectPanel(owner, sm, mapName,
                                              model.entitySpec,
                                              allowPlayer = true,
                                              allowEventOnOtherMap = false)

  val fieldDx = new FloatSpinner(-100, 100, 0.1f, model.dx, model.dx = _)
  val fieldDy = new FloatSpinner(-100, 100, 0.1f, model.dy, model.dy = _)

  val fieldAffixDirection =
    boolField("", model.affixDirection, model.affixDirection = _)
  val fieldAsync =
    boolField("", model.async, model.async = _)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += fieldWhichEvent
    contents += new DesignGridPanel {
      row().grid(lbl(getMessageColon("X_Movement"))).add(fieldDx)
      row().grid(lbl(getMessageColon("Y_Movement"))).add(fieldDy)
      row().grid(lbl(getMessageColon("Affix_direction"))).add(fieldAffixDirection)
      row().grid(lbl(getMessageColon("Async"))).add(fieldAsync)
      addButtons(okBtn, cancelBtn)
    }
  }
}