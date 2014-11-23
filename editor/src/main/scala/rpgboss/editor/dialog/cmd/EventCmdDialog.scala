package rpgboss.editor.dialog.cmd

import scala.swing.Component
import scala.swing.Window

import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.ParameterFullComponent
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.lib.Utils
import rpgboss.model.event.AddRemoveGold
import rpgboss.model.event.AddRemoveItem
import rpgboss.model.event.EventCmd
import rpgboss.model.event.ModifyParty
import rpgboss.model.event.MoveEvent
import rpgboss.model.event.RunJs
import rpgboss.model.event.SetEventState
import rpgboss.model.event.ShowText
import rpgboss.model.event.StartBattle
import rpgboss.model.event.Teleport

abstract class EventCmdDialog[T <: EventCmd](
  owner: Window,
  sm: StateMaster,
  title: String,
  initial: T,
  successF: T => Any)(implicit m: reflect.Manifest[T])
  extends StdDialog(owner, title) {

  case class TitledComponent(title: String, component: Component)

  def fields: Seq[TitledComponent]

  val model = Utils.deepCopy(initial)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    for (TitledComponent(fieldName, fieldComponent) <- fields) {
      row().grid(lbl(fieldName)).add(fieldComponent)
    }
    ParameterFullComponent.addParameterFullComponentsToPanel(
        owner, sm.getProjData, this, model)

    addButtons(okBtn, cancelBtn)
  }
}

object EventCmdDialog {
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
    successF: (EventCmd) => Any) = {
    evtCmd match {
      case e: AddRemoveItem =>
        new AddRemoveItemCmdDialog(owner, sm, e, successF)
      case e: AddRemoveGold =>
        new AddRemoveGoldCmdDialog(owner, sm, e, successF)
      case e: ModifyParty => new ModifyPartyCmdDialog(owner, sm, e, successF)
      case e: ShowText => new ShowTextCmdDialog(owner, e, successF)
      case e: Teleport => new TeleportCmdDialog(owner, sm, e, successF)
      case e: SetEventState =>
        new SetEventStateDialog(owner, sm, mapName, e, successF)
      case e: MoveEvent =>
        new MoveEventCmdDialog(owner, sm, mapName, e, successF)
      case e: StartBattle =>
        new StartBattleCmdDialog(owner, sm, e, successF)
      case e: RunJs => new RunJsCmdDialog(owner, e, successF)
      case _ => null
    }
  }
}