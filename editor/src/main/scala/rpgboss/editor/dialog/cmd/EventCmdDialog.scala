package rpgboss.editor.dialog.cmd

import scala.swing.Component
import scala.swing.Window
import rpgboss.editor.Internationalized._
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.ParameterFullComponent
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.lib.Utils
import rpgboss.model.event._
import rpgboss.model.ProjectData
import rpgboss.editor.uibase.EventParameterField
import rpgboss.editor.uibase.EventParameterField._
import rpgboss.model.AddOrRemove
import scala.swing.Dialog

case class TitledComponent(title: String, component: Component)

abstract class EventCmdDialog[T <: EventCmd](
  owner: Window,
  sm: StateMaster,
  title: String,
  initial: T,
  successF: T => Any)(implicit m: reflect.Manifest[T])
  extends StdDialog(owner, title) {

  def normalFields: Seq[TitledComponent] = Nil
  def parameterFields: Seq[EventParameterField[_]] = Nil

  val model: T = Utils.deepCopy(initial)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    for (TitledComponent(fieldName, fieldComponent) <- normalFields) {
      row().grid(lbl(fieldName)).add(fieldComponent)
    }
    ParameterFullComponent.addParameterFullComponentsToPanel(
        owner, this, parameterFields)

    addButtons(okBtn, cancelBtn)
  }
}

object EventCmdDialog {
  def eventCmdUis: Seq[EventCmdUI[_]] = List(
      AddRemoveItemUI,
      AddRemoveGoldUI,
      GetChoiceUI,
      HealOrDamageUI,
      HidePictureUI,
      IfConditionUI,
      ModifyPartyUI,
      MoveEventUI,
      OpenStoreUI,
      PlayMusicUI,
      PlaySoundUI,
      RunJsUI,
      SetEventStateUI,
      SetGlobalIntUI,
      ShowPictureUI,
      ShowTextUI,
      StartBattleUI,
      StopMusicUI,
      TeleportUI,
      TintScreenUI,
      WhileLoopUI)

  def uiFor(cmd: EventCmd): EventCmdUI[_] = {
    for (ui <- eventCmdUis) {
      if (ui.m.runtimeClass == cmd.getClass())
        return ui
    }
    return null
  }

  /**
   * This function gets a dialog for the given EventCmd
   */
  def dialogFor(
    owner: Window,
    sm: StateMaster,
    mapName: Option[String],
    evtCmd: EventCmd,
    successF: EventCmd => Any): Dialog = {
    val ui = uiFor(evtCmd)
    ui.getDialog(owner, sm, mapName,
        evtCmd.asInstanceOf[ui.EventCmdType],
        successF.asInstanceOf[ui.EventCmdType => Any])
  }
}

abstract class EventCmdUI[T <: EventCmd](implicit val m: reflect.Manifest[T]) {
  type EventCmdType = T

  def title: String
  def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: EventCmdType):
      Seq[TitledComponent] = Nil
  def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: EventCmdType):
      Seq[EventParameterField[_]] = Nil

  def getDialog(
      owner: Window, sm: StateMaster, mapName: Option[String], initial: EventCmdType,
      successF: EventCmdType => Any) =
    new EventCmdDialog(owner, sm, title, initial, successF) {
      override def normalFields =
        EventCmdUI.this.getNormalFields(owner, sm, mapName, model)
      override def parameterFields =
        EventCmdUI.this.getParameterFields(owner, sm, mapName, model)
    }
}