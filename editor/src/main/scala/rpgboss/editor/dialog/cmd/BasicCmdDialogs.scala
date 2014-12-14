package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._
import rpgboss.editor.resourceselector.BattleBackgroundField
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove
import rpgboss.editor.Internationalized._
import rpgboss.lib.ArrayUtils
import rpgboss.editor.resourceselector.PictureField

class StartBattleCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: StartBattle,
  successF: (StartBattle) => Any)
  extends StdDialog(owner, getMessage("StartBattle")) {

  centerDialog(new Dimension(300, 200))

  var model = Utils.deepCopy(initial)

  val encounterSelect = indexedCombo(
    sm.getProjData.enums.encounters,
    model.encounterId,
    v => model = model.copy(encounterId = v))

  val battleBgSelect = new BattleBackgroundField(
    owner,
    sm,
    model.battleBackground,
    v => model = model.copy(battleBackground = v))

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel(getMessageColon("Encounter")))
    row().grid().add(encounterSelect)

    row().grid().add(leftLabel(getMessageColon("Battle_Background")))
    row().grid().add(battleBgSelect)

    addButtons(okBtn, cancelBtn)
  }
}

class AddRemoveItemCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: AddRemoveItem,
  successF: (AddRemoveItem) => Any)
  extends EventCmdDialog(owner, sm, getMessage("Add_Remove_Item"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add, model.add = _))
  )
}

class AddRemoveGoldCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: AddRemoveGold,
  successF: (AddRemoveGold) => Any)
  extends EventCmdDialog(owner, sm, getMessage("Add_Remove_Item"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add, model.add = _))
  )
}

class GetChoiceCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: GetChoice,
  successF: (GetChoice) => Any)
  extends EventCmdDialog(owner, sm, getMessage("Get_Choice"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent("Question",
        textAreaField(model.question, model.question = _)),
    TitledComponent("", new StringArrayEditingPanel(
        owner, "Choices", model.choices,
        newChoices => {
          model.choices = newChoices
          model.innerCmds = ArrayUtils.resized(model.innerCmds, newChoices.size,
              () => Array[EventCmd]())
        },
        minElems = 2, maxElems = 4)),
    TitledComponent("", boolField("Allow Cancel", model.allowCancel,
        model.allowCancel = _)))
}

class HidePictureCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: HidePicture,
  successF: (HidePicture) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Hide Picture"), initial, successF)

class OpenStoreCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: OpenStore,
  successF: (OpenStore) => Any)
  extends EventCmdDialog(owner, sm, getMessage("Open_Store"), initial, successF)

class ShowPictureCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: ShowPicture,
  successF: (ShowPicture) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Show Picture"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          needsTranslation("Picture"),
          new PictureField(owner, sm, model.picture, model.picture = _)),
      TitledComponent(
          needsTranslation("Layout"),
          new LayoutEditingPanel(model.layout)))
}
