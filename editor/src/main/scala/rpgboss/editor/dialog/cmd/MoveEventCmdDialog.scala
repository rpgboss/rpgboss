package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.StateMaster
import rpgboss.lib.Utils

class MoveEventCmdDialog(
  owner: Window,
  sm: StateMaster,
  mapName: Option[String],
  initial: MoveEvent,
  successF: (MoveEvent) => Any)
  extends StdDialog(owner, "Move event") {

  centerDialog(new Dimension(200, 300))

  val model = Utils.deepCopy(initial)

  val fieldWhichEvent = new EntitySelectPanel(owner, sm, mapName,
                                              model.entitySpec,
                                              allowPlayer = true,
                                              allowEventOnOtherMap = false)

  val fieldDx = new FloatSpinner(model.dx, -100, 100, model.dx = _, 0.1f)
  val fieldDy = new FloatSpinner(model.dy, -100, 100, model.dy = _, 0.1f)

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
      row().grid(lbl("X Movement:")).add(fieldDx)
      row().grid(lbl("Y Movement:")).add(fieldDy)
      row().grid(lbl("Affix direction:")).add(fieldAffixDirection)
      row().grid(lbl("Async:")).add(fieldAsync)
      addButtons(okBtn, cancelBtn)
    }
  }
}