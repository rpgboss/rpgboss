package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove
import rpgboss.model.HealOrDamageEnum
import rpgboss.editor.Internationalized._
import rpgboss.lib.ArrayUtils
import rpgboss.editor.resourceselector._
import rpgboss.editor.dialog.ConditionsPanel

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

    row().grid().add(leftLabel(getMessageColon("Override_Battle_Music")))
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
    TitledComponent(getMessage("Question"),
        textAreaField(model.question, model.question = _)),
    TitledComponent("", new StringArrayEditingPanel(
        owner, getMessage("Choices"), model.choices,
        newChoices => {
          model.choices = newChoices
          model.innerCmds = ArrayUtils.resized(model.innerCmds, newChoices.size,
              () => Array[EventCmd]())
        },
        minElems = 2, maxElems = 4)),
    TitledComponent("", boolField(getMessage("Allow_Cancel"), model.allowCancel,
        model.allowCancel = _)))
}

class HealOrDamageCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: HealOrDamage,
  successF: (HealOrDamage) => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Heal/Damage"), initial, successF) {
  override def extraFields = Seq(
    TitledComponent("", boolEnumHorizBox(
        HealOrDamageEnum, model.heal, model.heal = _)),
    TitledComponent("", boolField(
        "Whole party", model.wholeParty, model.wholeParty = _)),
    TitledComponent("Character", indexedCombo(
        sm.getProjData.enums.characters, model.characterId,
        model.characterId = _)),
    TitledComponent("HP Percentage", percentField(0.01f, 1, model.hpPercentage,
        model.hpPercentage = _)),
    TitledComponent("MP Percentage", percentField(0.01f, 1, model.mpPercentage,
        model.mpPercentage = _)))
}


class HidePictureCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: HidePicture,
  successF: (HidePicture) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Hide_Picture"), initial, successF)

class IfConditionCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: IfCondition,
  successF: (IfCondition) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("IF_Condition"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)),
    TitledComponent("", boolField(getMessage("ELSE_Branch"),
        model.elseBranch, model.elseBranch = _)))
}

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
      owner, sm, getMessage("Play_Music"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          getMessage("Music"),
          new MusicField(owner, sm, Some(model.spec), v => model.spec = v.get,
              allowNone = false)),
      TitledComponent(
          "",
          boolField(getMessage("Loop"), model.loop, model.loop = _)),
      TitledComponent(
          getMessage("Fade_Duration"),
          new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
              model.fadeDuration = _)))
}

class PlaySoundCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: PlaySound,
  successF: (PlaySound) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Play_Sound"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          getMessage("Sound"),
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
      owner, sm, getMessage("Stop_Music"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          getMessage("Fade_Duration"),
          new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
              model.fadeDuration = _)))
}

class TintScreenCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: TintScreen,
  successF: TintScreen => Any)
  extends EventCmdDialog(
      owner, sm, needsTranslation("Tint Screen"), initial, successF) {
  override def extraFields = Seq(
      TitledComponent(
          needsTranslation("Color and alpha:"),
          colorField(
              (initial.r, initial.g, initial.b, initial.a),
              (r, g, b, a) => {
                model.r = r
                model.g = g
                model.b = b
                model.a = a
              })),
      TitledComponent(needsTranslation("Fade duration:"),
          new FloatSpinner(
              0, 10f, 0.1f, model.fadeDuration, model.fadeDuration = _)))
}

class WhileLoopCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: WhileLoop,
  successF: (WhileLoop) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("While_Loop"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)))
}
