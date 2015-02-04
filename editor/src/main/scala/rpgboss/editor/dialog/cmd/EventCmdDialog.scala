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
  type EventCmdType = T

  def normalFields: Seq[TitledComponent] = Nil
  def parameterFields: Seq[EventParameterField[_]] = Nil

  val model: EventCmdType = Utils.deepCopy(initial)

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
  def uiFor(cmd: EventCmd): EventCmdUI[_] = cmd match {
    case e: AddRemoveItem => AddRemoveItemUI
    case e => null
  }

  /**
   * This function gets a dialog for the given EventCmd
   *
   * One may argue here that it's not object oriented to case match through
   * all the possible types searching for the right dialog, and that we should
   * use polymorphism.
   *
   * I generally agree, but feel that adding UI details to the model is
   * more disgusting than this hack.
   */
  def dialogFor(
    owner: Window,
    sm: StateMaster,
    mapName: Option[String],
    evtCmd: EventCmd,
    successF: EventCmd => Any): Dialog = {
    evtCmd match {
      case e: AddRemoveGold =>
        new AddRemoveGoldCmdDialog(owner, sm, e, successF)
      case e: GetChoice => new GetChoiceCmdDialog(owner, sm, e, successF)
      case e: HealOrDamage => new HealOrDamageCmdDialog(owner, sm, e, successF)
      case e: HidePicture => new HidePictureCmdDialog(owner, sm, e, successF)
      case e: IfCondition => new IfConditionCmdDialog(owner, sm, e, successF)
      case e: ModifyParty => new ModifyPartyCmdDialog(owner, sm, e, successF)
      case e: OpenStore => new OpenStoreCmdDialog(owner, sm, e, successF)
      case e: SetGlobalInt => new SetGlobalIntDialog(owner, sm, e, successF)
      case e: ShowText => new ShowTextCmdDialog(owner, sm, e, successF)
      case e: ShowPicture => new ShowPictureCmdDialog(owner, sm, e, successF)
      case e: Teleport => new TeleportCmdDialog(owner, sm, e, successF)
      case e: SetEventState =>
        new SetEventStateDialog(owner, sm, mapName, e, successF)
      case e: MoveEvent =>
        new MoveEventCmdDialog(owner, sm, mapName, e, successF)
      case e: PlayMusic => new PlayMusicCmdDialog(owner, sm, e, successF)
      case e: PlaySound => new PlaySoundCmdDialog(owner, sm, e, successF)
      case e: StartBattle =>
        new StartBattleCmdDialog(owner, sm, e, successF)
      case e: StopMusic => new StopMusicCmdDialog(owner, sm, e, successF)
      case e: TintScreen => new TintScreenCmdDialog(owner, sm, e, successF)
      case e: RunJs => new RunJsCmdDialog(owner, e, successF)
      case e: WhileLoop => new WhileLoopCmdDialog(owner, sm, e, successF)
      case e =>
        val ui = uiFor(evtCmd)
        ui.getDialog(owner, sm, evtCmd.asInstanceOf[ui.EventCmdType],
            successF.asInstanceOf[ui.EventCmdType => Any])
    }
  }
}

abstract class EventCmdUI[T <: EventCmd](implicit m: reflect.Manifest[T]) {
  type EventCmdType = T

  def dialogTitle: String
  def getNormalFields(
      owner: Window, pData: ProjectData, model: EventCmdType):
      Seq[TitledComponent] = Nil
  def getParameterFields(
      owner: Window, pData: ProjectData, model: EventCmdType):
      Seq[EventParameterField[_]] = Nil

  def getDialog(
      owner: Window, sm: StateMaster, initial: EventCmdType,
      successF: EventCmdType => Any) =
    new EventCmdDialog(owner, sm, dialogTitle, initial, successF) {
      override def normalFields =
        EventCmdUI.this.getNormalFields(owner, sm.getProjData, model)
      override def parameterFields =
        EventCmdUI.this.getParameterFields(owner, sm.getProjData, model)
    }
}


object AddRemoveItemUI
  extends EventCmdUI[AddRemoveItem] {
  override def dialogTitle = getMessage("Add_Remove_Item")
  override def getNormalFields(
      owner: Window, pData: ProjectData, model: AddRemoveItem) = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add, model.add = _))
  )
  override def getParameterFields(
      owner: Window, pData: ProjectData, model: AddRemoveItem) = List(
    IntEnumIdField(getMessage("Item"), pData.enums.items, model.itemId),
    IntNumberField(getMessage("Quantity"), 1, 99, model.quantity))
}