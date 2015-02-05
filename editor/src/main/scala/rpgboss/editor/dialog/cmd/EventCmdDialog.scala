package rpgboss.editor.dialog.cmd

import scala.swing.Dialog
import scala.swing.Window

import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.EventParameterField
import rpgboss.editor.uibase.ParameterFullComponent
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.lib.Utils
import rpgboss.model.event.EventCmd

abstract class EventCmdDialog[T <: EventCmd](
  owner: Window,
  sm: StateMaster,
  title: String,
  initial: T,
  successF: T => Any)(implicit m: reflect.Manifest[T])
  extends StdDialog(owner, title) {

  def normalFields: Seq[EventField] = Nil
  def parameterFields: Seq[EventParameterField[_]] = Nil

  val model: T = Utils.deepCopy(initial)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    for (EventField(fieldName, fieldComponent) <- normalFields) {
      row().grid(lbl(fieldName)).add(fieldComponent)
    }
    ParameterFullComponent.addParameterFullComponentsToPanel(
        owner, this, parameterFields)

    addButtons(okBtn, cancelBtn)
  }
}

object EventCmdDialog {
  /**
   * This function gets a dialog for the given EventCmd
   */
  def dialogFor(
    owner: Window,
    sm: StateMaster,
    mapName: Option[String],
    evtCmd: EventCmd,
    successF: EventCmd => Any): Option[Dialog] = {
    val ui = EventCmdUI.uiFor(evtCmd)
    ui.getDialog(owner, sm, mapName,
        evtCmd.asInstanceOf[ui.EventCmdType],
        successF.asInstanceOf[ui.EventCmdType => Any])
  }
}