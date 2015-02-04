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
import rpgboss.model.ProjectData
import rpgboss.editor.uibase.EventParameterField
import rpgboss.editor.uibase.EventParameterField._
import rpgboss.model.PictureSlots
import rpgboss.player.RpgScreen

object AddRemoveItemUI extends EventCmdUI[AddRemoveItem] {
  override def title = getMessage("Add_Remove_Item")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveItem) = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add,
        model.add = _)))
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveItem) = List(
    IntEnumIdField(getMessage("Item"), sm.getProjData.enums.items, model.itemId),
    IntNumberField(getMessage("Quantity"), 1, 99, model.quantity))
}

object AddRemoveGoldUI extends EventCmdUI[AddRemoveGold] {
  override def title = getMessage("Add_Remove_Gold")

  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveGold) = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add,
        model.add = _)))
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveGold) = List(
    IntNumberField(getMessage("Quantity"), 1, 9999, model.quantity))
}

object GetChoiceUI extends EventCmdUI[GetChoice] {
  override def title = getMessage("Get_Choice")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: GetChoice) = Seq(
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

object HealOrDamageUI extends EventCmdUI[HealOrDamage] {
  override def title = getMessage("Heal_Damage")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: HealOrDamage) = Seq(
    TitledComponent("", boolEnumHorizBox(
        HealOrDamageEnum, model.heal, model.heal = _)),
    TitledComponent("", boolField(
        needsTranslation("Whole party"), model.wholeParty, model.wholeParty = _)),
    TitledComponent("", boolField(
        needsTranslation("Cure status effects (heal only)"),
        model.removeStatusEffects, model.removeStatusEffects = _)),
    TitledComponent("Character", indexedCombo(
        sm.getProjData.enums.characters, model.characterId,
        model.characterId = _)),
    TitledComponent("HP Percentage", percentField(0.01f, 1, model.hpPercentage,
        model.hpPercentage = _)),
    TitledComponent("MP Percentage", percentField(0.01f, 1, model.mpPercentage,
        model.mpPercentage = _)))
}

object HidePictureUI extends EventCmdUI[HidePicture] {
  override def title = getMessage("Hide_Picture")
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: HidePicture) = List(
    IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
        PictureSlots.BATTLE_BEGIN - 1, model.slot))
}

object IfConditionUI extends EventCmdUI[IfCondition] {
  override def title = getMessage("IF_Condition")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: IfCondition) = Seq(
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)),
    TitledComponent("", boolField(getMessage("ELSE_Branch"),
        model.elseBranch, model.elseBranch = _)))
}

object ModifyPartyUI extends EventCmdUI[ModifyParty] {
  override def title = getMessage("Modify_Party")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: ModifyParty) = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add,
        model.add = _)),
    TitledComponent(getMessage("Character"), indexedCombo(
        sm.getProjData.enums.characters, model.characterId,
        model.characterId = _)))
}

object MoveEventUI extends EventCmdUI[MoveEvent] {
  override def title = getMessage("Move_Event")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: MoveEvent) = Seq(
    TitledComponent("", new EntitySelectPanel(owner, sm, mapName,
        model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)),
    TitledComponent(getMessage("X_Movement"),
        new FloatSpinner(-100, 100, 0.1f, model.dx, model.dx = _)),
    TitledComponent(getMessage("Y_Movement"),
        new FloatSpinner(-100, 100, 0.1f, model.dy, model.dy = _)),
    TitledComponent("", boolField(getMessage("Affix_direction"),
        model.affixDirection, model.affixDirection = _)),
    TitledComponent("", boolField(getMessage("Async"), model.async,
        model.async = _)))
}

object OpenStoreUI extends EventCmdUI[OpenStore] {
  override def title = getMessage("Open_Store")
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: OpenStore) = List(
    IntMultiselectField(owner, getMessage("Items_Sold"),
        sm.getProjData.enums.items, model.itemIdsSold),
    FloatPercentField(getMessageColon("Buy_Price_Multiplier"), 0f, 4f,
        model.buyPriceMultiplier),
    FloatPercentField(getMessageColon("Sell_Price_Multiplier"), 0f, 4f,
        model.sellPriceMultiplier))
}

object PlayMusicUI extends EventCmdUI[PlayMusic] {
  override def title = getMessage("Play_Music")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: PlayMusic) = Seq(
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
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: PlayMusic) = List(
    IntNumberField(getMessage("Slot"), 0, RpgScreen.MAX_MUSIC_SLOTS,
        model.slot))
}

object PlaySoundUI extends EventCmdUI[PlaySound] {
  override def title = getMessage("Play_Sound")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: PlaySound) = Seq(
    TitledComponent(
        getMessage("Sound"),
        new SoundField(owner, sm, Some(model.spec), v => model.spec = v.get,
            allowNone = false)))
}

object SetGlobalIntUI extends EventCmdUI[SetGlobalInt] {
  override def title = getMessage("Set_Global_Integer")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: SetGlobalInt) = Seq(
    TitledComponent(
        getMessage("Global_Variable_Name"),
        textField(model.key, model.key = _)),
    TitledComponent(
        getMessage("Operation"),
        enumVerticalBox(
            OperatorType, model.operatorId, model.operatorId = _)))
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: SetGlobalInt) = List(
    IntNumberField(getMessage("Value") + " 1", -9999, 9999, model.value1),
    IntNumberField(getMessage("Value") + " 2", -9999, 9999, model.value2))
}

object ShowPictureUI extends EventCmdUI[ShowPicture] {
  override def title = getMessage("Show_Picture")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: ShowPicture) = Seq(
    TitledComponent(
        getMessage("Picture"),
        new PictureField(owner, sm, model.picture, model.picture = _)),
    TitledComponent(
        getMessage("Layout"),
        new LayoutEditingPanel(model.layout)))
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: ShowPicture) = List(
    IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
        PictureSlots.BATTLE_BEGIN - 1, model.slot))
}

object StartBattleUI extends EventCmdUI[StartBattle] {
  override def title = getMessage("StartBattle")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: StartBattle) = Seq(
    TitledComponent(
        getMessage("Encounter"),
        indexedCombo(sm.getProjData.enums.encounters, model.encounterId,
            model.encounterId = _)),
    TitledComponent(
        getMessage("Override_Battle_Background"),
        new BattleBackgroundField(
            owner,
            sm,
            model.battleBackground,
            model.battleBackground = _)),
    TitledComponent(
        getMessage("Override_Battle_Music"),
        new MusicField(owner, sm, model.battleMusic, model.battleMusic = _)))
}

object StopMusicUI extends EventCmdUI[StopMusic] {
  override def title = getMessage("Stop_Music")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: StopMusic) = Seq(
      TitledComponent(
          getMessage("Fade_Duration"),
          new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
              model.fadeDuration = _)))
  override def getParameterFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: StopMusic) = List(
    IntNumberField(getMessage("Slot"), 0, RpgScreen.MAX_MUSIC_SLOTS,
        model.slot))
}

object TintScreenUI extends EventCmdUI[TintScreen] {
  override def title = getMessage("Tint_Screen")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: TintScreen) = Seq(
    TitledComponent(
        getMessageColon("Color_And_Alpha"),
        colorField(
            (model.r, model.g, model.b, model.a),
            (r, g, b, a) => {
              model.r = r
              model.g = g
              model.b = b
              model.a = a
            })),
    TitledComponent(getMessageColon("Fade_Duration"),
        new FloatSpinner(
            0, 10f, 0.1f, model.fadeDuration, model.fadeDuration = _)))
}

object WhileLoopUI extends EventCmdUI[WhileLoop] {
  override def title = getMessage("While_Loop")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: WhileLoop) = Seq(
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)))
}
