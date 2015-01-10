package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove
import rpgboss.editor.Internationalized._
import rpgboss.lib.ArrayUtils
import rpgboss.editor.resourceselector._

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
    model.encounterId = _)

  val battleBgSelect = new BattleBackgroundField(
    owner,
    sm,
    model.battleBackground,
    model.battleBackground = _)

  val battleMusicField =
    new MusicField(owner, sm, model.battleMusic, model.battleMusic = _)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel(getMessageColon("Encounter")))
    row().grid().add(encounterSelect)

    row().grid().add(leftLabel(getMessageColon("Override_Battle_Background")))
    row().grid().add(battleBgSelect)

    row().grid().add(leftLabel(needsTranslation("Override Battle Music:")))
    row().grid().add(battleMusicField)

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
    TitledComponent("", boolField(getMessage("Allow_Cancel"), model.allowCancel,
        model.allowCancel = _)))
}

class HidePictureCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: HidePicture,
  successF: (HidePicture) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Hide_Picture"), initial, successF)

class OpenStoreCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: OpenStore,
  successF: (OpenStore) => Any)
  extends EventCmdDialog(owner, sm, getMessage("Open_Store"), initial, successF)

class PlayMusicCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: PlayMusic,
  successF: (PlayMusic) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Play Music"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          needsTranslation("Music"),
          new MusicField(owner, sm, Some(model.spec), v => model.spec = v.get,
              allowNone = false)),
      TitledComponent(
          "",
          boolField(needsTranslation("Loop"), model.loop, model.loop = _)),
      TitledComponent(
          needsTranslation("Fade duration"),
          new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
              model.fadeDuration = _)))
}

class PlaySoundCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: PlaySound,
  successF: (PlaySound) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Play Sound"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          needsTranslation("Sound"),
          new SoundField(owner, sm, Some(model.spec), v => model.spec = v.get,
              allowNone = false)))
}

class SetGlobalIntDialog(
  owner: Window,
  sm: StateMaster,
  initial: SetGlobalInt,
  successF: (SetGlobalInt) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Set_Global_Integer"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          getMessage("Global_Variable_Name"),
          textField(model.key, model.key = _)),
      TitledComponent(
          getMessage("Operation"),
          enumVerticalBox(
              OperatorType, model.operatorId, model.operatorId = _)))
}

class ShowPictureCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: ShowPicture,
  successF: (ShowPicture) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Show_Picture"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          getMessage("Picture"),
          new PictureField(owner, sm, model.picture, model.picture = _)),
      TitledComponent(
          getMessage("Layout"),
          new LayoutEditingPanel(model.layout)))
}

class StopMusicCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: StopMusic,
  successF: (StopMusic) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Stop Music"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          needsTranslation("Fade duration"),
          new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
              model.fadeDuration = _)))
}
