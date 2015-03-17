package rpgboss.editor.dialog.cmd

import scala.swing.Component
import scala.swing.Dialog
import scala.swing.Window
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import rpgboss.editor.Internationalized.getMessage
import rpgboss.editor.Internationalized.getMessageColon
import rpgboss.editor.Internationalized.needsTranslation
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.ConditionsPanel
import rpgboss.editor.dialog.cmd.EventCmdCategory.Effects
import rpgboss.editor.dialog.cmd.EventCmdCategory.Battles
import rpgboss.editor.dialog.cmd.EventCmdCategory.Inventory
import rpgboss.editor.dialog.cmd.EventCmdCategory.Movement
import rpgboss.editor.dialog.cmd.EventCmdCategory.Party
import rpgboss.editor.dialog.cmd.EventCmdCategory.Programming
import rpgboss.editor.dialog.cmd.EventCmdCategory.Windows
import rpgboss.editor.misc.MapLocPanel
import rpgboss.editor.resourceselector.BattleBackgroundField
import rpgboss.editor.resourceselector.MusicField
import rpgboss.editor.resourceselector.PictureField
import rpgboss.editor.resourceselector.SoundField
import rpgboss.editor.resourceselector.WindowskinField
import rpgboss.editor.uibase.EntitySelectPanel
import rpgboss.editor.uibase.EventParameterField
import rpgboss.editor.uibase.EventParameterField.FloatPercentField
import rpgboss.editor.uibase.EventParameterField.IntEnumIdField
import rpgboss.editor.uibase.EventParameterField.IntMultiselectField
import rpgboss.editor.uibase.EventParameterField.IntNumberField
import rpgboss.editor.uibase.FloatSpinner
import rpgboss.editor.uibase.LayoutEditingPanel
import rpgboss.editor.uibase.NumberSpinner
import rpgboss.editor.uibase.StringArrayEditingPanel
import rpgboss.editor.uibase.SwingUtils.boolEnumHorizBox
import rpgboss.editor.uibase.SwingUtils.boolField
import rpgboss.editor.uibase.SwingUtils.colorField
import rpgboss.editor.uibase.SwingUtils.enumVerticalBox
import rpgboss.editor.uibase.SwingUtils.indexedCombo
import rpgboss.editor.uibase.SwingUtils.percentField
import rpgboss.editor.uibase.SwingUtils.textAreaField
import rpgboss.editor.uibase.SwingUtils.textField
import rpgboss.lib.ArrayUtils
import rpgboss.model.AddOrRemove
import rpgboss.model.HealOrDamageEnum
import rpgboss.model.EntityInfoEnum
import rpgboss.model.EnabledDisabledEnum
import rpgboss.model.MapLoc
import rpgboss.model.PictureSlots
import rpgboss.model.RpgMapData
import rpgboss.model.Transitions
import rpgboss.model.event._
import rpgboss.player.RpgScreen
import rpgboss.model.Origins
import rpgboss.model.ColorSpec
import rpgboss.model.WeatherTypes
import rpgboss.model.MusicSlots
import rpgboss.editor.resourceselector.FaceField

case class EventField(title: String, component: Component)

object EventCmdUI {
  val eventCmdUis: Seq[EventCmdUI[_]] = List(
    AddRemoveItemUI,
    AddRemoveGoldUI,
    BreakLoopUI,
    CallMenuUI,
    CallSaveMenuUI,
    ChangeCameraFollowUI,
    CommentUI,
    ClearTimerUI,
    ExitGameUI,
    FadeInUI,
    FadeOutUI,
    GameOverUI,
    GetChoiceUI,
    GetEntityInfoUI,
    HealOrDamageUI,
    HidePictureUI,
    IfConditionUI,
    LockPlayerMovementUI,
    EnableDisableMenuUI,
    ModifyPartyUI,
    MoveCameraUI,
    MoveEventUI,
    OpenStoreUI,
    PlayAnimationUI,
    PlayMusicUI,
    PlaySoundUI,
    RunJsUI,
    SetEventSpeedUI,
    SetEventStateUI,
    SetGlobalIntUI,
    SetTimerUI,
    SetTransitionUI,
    SetWindowskinUI,
    StopSoundUI,
    ShowPictureUI,
    ShowTextUI,
    StartBattleUI,
    StopMusicUI,
    TeleportUI,
    TintScreenUI,
    WeatherEffectsUI,
    WhileLoopUI,
    SleepUI)

  def uiFor(cmd: EventCmd): EventCmdUI[_] = {
    for (ui <- eventCmdUis) {
      if (ui.m.runtimeClass == cmd.getClass())
        return ui
    }
    return null
  }
}

abstract class EventCmdUI[T <: EventCmd](implicit val m: reflect.Manifest[T]) {
  type EventCmdType = T

  def category: EventCmdCategory.Value
  def title: String
  def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String],
    model: EventCmdType): Seq[EventField] = Nil
  def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String],
    model: EventCmdType): Seq[EventParameterField[_]] = Nil

  /**
   * @param     Location of the event this UI is for.
   */
  def newInstance(eventLoc: Option[MapLoc]): EventCmd = {
    // This is the only way I've been able to discover to leverage the default
    // constructor of every EventCmd subclass.
    parse("{}").extract[EventCmdType](RpgMapData.formats, m)
  }

  def getDialog(
    ownerArg: Window, sm: StateMaster, mapName: Option[String],
    initial: EventCmdType, successF: EventCmdType => Any): Option[Dialog] = {

    // ownerArg is named differently from owner, to prevent EventCmdDialog.owner
    // (which is still null), from shadowing the non-null function argument
    val dialog = new EventCmdDialog(ownerArg, sm, title, initial, successF) {
      override def normalFields = getNormalFields(ownerArg, sm, mapName, model)
      override def parameterFields =
        getParameterFields(ownerArg, sm, mapName, model)
    }

    if (dialog.normalFields.isEmpty && dialog.parameterFields.isEmpty) {
      return None
    } else {
      return Some(dialog)
    }
  }
}

object AddRemoveItemUI extends EventCmdUI[AddRemoveItem] {
  override def category = Inventory
  override def title = getMessage("Add_Remove_Item")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveItem) = Seq(
    EventField("", boolEnumHorizBox(AddOrRemove, model.add,
      model.add = _)))
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveItem) = List(
    IntEnumIdField(getMessage("Item"), sm.getProjData.enums.items, model.itemId),
    IntNumberField(getMessage("Quantity"), 1, 99, model.quantity))
}

object AddRemoveGoldUI extends EventCmdUI[AddRemoveGold] {
  override def category = Inventory
  override def title = getMessage("Add_Remove_Gold")

  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveGold) = Seq(
    EventField("", boolEnumHorizBox(AddOrRemove, model.add,
      model.add = _)))
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: AddRemoveGold) = List(
    IntNumberField(getMessage("Quantity"), 1, 9999, model.quantity))
}

object BreakLoopUI extends EventCmdUI[BreakLoop] {
  override def category = Programming
  override def title = getMessage("Break_Loop")
}

object ExitGameUI extends EventCmdUI[ExitGame] {
  override def category = Programming
  override def title = getMessage("Exit_Game")
}

object GetChoiceUI extends EventCmdUI[GetChoice] {
  override def category = Windows
  override def title = getMessage("Get_Choice")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: GetChoice) = {
    val fCharacter = indexedCombo(
      sm.getProjData.enums.characters,
      model.characterId,
      model.characterId = _)
    val fFace = new FaceField(owner, sm, model.customFace, model.customFace = _)

    def setEnabledFields() = {
      fCharacter.enabled = model.useCharacterFace
      fFace.enabled = !model.useCharacterFace
    }
    setEnabledFields()

    Seq(
      EventField(getMessage("Question"),
        textAreaField(model.question, model.question = _)),
      EventField("", new StringArrayEditingPanel(
        owner, getMessage("Choices"), model.choices,
        newChoices => {
          model.choices = newChoices
          model.innerCmds = ArrayUtils.resized(model.innerCmds, newChoices.size,
            () => Array[EventCmd]())
        },
        minElems = 2, maxElems = 4)),
      EventField("", boolField(getMessage("Allow_Cancel"), model.allowCancel,
        model.allowCancel = _)),
      EventField(getMessage("Face"), fFace),
      EventField("", boolField(getMessage("Use_Character_Face"),
        model.useCharacterFace, model.useCharacterFace = _,
        Some(setEnabledFields))),
      EventField(getMessage("Character"), fCharacter))
  }
}

object EnableDisableMenuUI extends EventCmdUI[EnableDisableMenu] {
  override def category = Windows
  override def title = getMessage("Enable_Disable_Menu")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: EnableDisableMenu) = Seq(
    EventField("", enumVerticalBox(
      EnabledDisabledEnum, model.enabled, model.enabled = _)))
}

object SetTimerUI extends EventCmdUI[SetTimer] {
  override def category = Windows
  override def title = getMessage("Set_Timer")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: SetTimer) = Seq(
    EventField(getMessage("Minutes"),
      new FloatSpinner(0, 60, 1f, model.minutes, model.minutes = _)),
    EventField(getMessage("Seconds"),
      new FloatSpinner(0, 60, 1f, model.seconds, model.seconds = _)))
}

object ClearTimerUI extends EventCmdUI[ClearTimer] {
  override def category = Programming
  override def title = getMessage("Clear_Timer")
}

object GameOverUI extends EventCmdUI[GameOver] {
  override def category = Programming
  override def title = getMessage("Game_Over")
}

object CallSaveMenuUI extends EventCmdUI[CallSaveMenu] {
  override def category = Programming
  override def title = getMessage("Call_Save_Menu")
}

object CallMenuUI extends EventCmdUI[CallMenu] {
  override def category = Programming
  override def title = getMessage("Call_Menu")
}

object FadeInUI extends EventCmdUI[FadeIn] {
  override def category = Programming
  override def title = getMessage("Fade_In")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: FadeIn) = Seq(
    EventField(getMessage("Duration"),
      new FloatSpinner(0, 100, 0.1f, model.duration, model.duration = _)))
}

object FadeOutUI extends EventCmdUI[FadeOut] {
  override def category = Programming
  override def title = getMessage("Fade_Out")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: FadeOut) = Seq(
    EventField(getMessage("Duration"),
      new FloatSpinner(0, 100, 0.1f, model.duration, model.duration = _)))
}

object WeatherEffectsUI extends EventCmdUI[WeatherEffects] {
  override def category = Party
  override def title = getMessage("Weather_Effects")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: WeatherEffects) = Seq(
    EventField("", enumVerticalBox(WeatherTypes, model.weatherTypeId,
      model.weatherTypeId = _)))
}

object HealOrDamageUI extends EventCmdUI[HealOrDamage] {
  override def category = Party
  override def title = getMessage("Heal_Damage")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: HealOrDamage) = Seq(
    EventField("", boolEnumHorizBox(
      HealOrDamageEnum, model.heal, model.heal = _)),
    EventField("", boolField(
      getMessage("Whole_Party"), model.wholeParty, model.wholeParty = _)),
    EventField("", boolField(
      getMessage("Cure_Status_Effects"),
      model.removeStatusEffects, model.removeStatusEffects = _)),
    EventField(getMessage("Character"), indexedCombo(
      sm.getProjData.enums.characters, model.characterId,
      model.characterId = _)),
    EventField(getMessage("HP_Percentage"), percentField(0.01f, 1, model.hpPercentage,
      model.hpPercentage = _)),
    EventField(getMessage("MP_Percentage"), percentField(0.01f, 1, model.mpPercentage,
      model.mpPercentage = _)))
}

object HidePictureUI extends EventCmdUI[HidePicture] {
  override def category = Windows
  override def title = getMessage("Hide_Picture")
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: HidePicture) = List(
    IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
      PictureSlots.BATTLE_BEGIN - 1, model.slot))
}

object IfConditionUI extends EventCmdUI[IfCondition] {
  override def category = Programming
  override def title = getMessage("IF_Condition")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: IfCondition) = Seq(
    EventField(getMessage("Conditions"),
      new ConditionsPanel(owner, sm.getProjData, model.conditions,
        model.conditions = _)),
    EventField("", boolField(getMessage("ELSE_Branch"),
      model.elseBranch, model.elseBranch = _)))
}

object LockPlayerMovementUI extends EventCmdUI[LockPlayerMovement] {
  override def category = Movement
  override def title = getMessage("Lock_Player_Movement")
}

object ModifyPartyUI extends EventCmdUI[ModifyParty] {
  override def category = Party
  override def title = getMessage("Modify_Party")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: ModifyParty) = Seq(
    EventField("", boolEnumHorizBox(AddOrRemove, model.add,
      model.add = _)),
    EventField(getMessage("Character"), indexedCombo(
      sm.getProjData.enums.characters, model.characterId,
      model.characterId = _)))
}

object MoveEventUI extends EventCmdUI[MoveEvent] {
  override def category = Movement
  override def title = getMessage("Move_Event")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: MoveEvent) = Seq(
    EventField("", new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)),
    EventField(getMessage("X_Movement"),
      new FloatSpinner(-100, 100, 1f, model.dx, model.dx = _)),
    EventField(getMessage("Y_Movement"),
      new FloatSpinner(-100, 100, 1f, model.dy, model.dy = _)),
    EventField("", boolField(getMessage("Affix_Direction"),
      model.affixDirection, model.affixDirection = _)),
    EventField("", boolField(getMessage("Async"), model.async,
      model.async = _)))
}

object GetEntityInfoUI extends EventCmdUI[GetEntityInfo] {
  override def category = Movement
  override def title = getMessage("Get_Entity_Info")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: GetEntityInfo) = Seq(
    EventField("", new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)),
    EventField(
      getMessage("Global_Variable_Name"),
      textField(model.globalVariableName, model.globalVariableName = _)),
    EventField("", enumVerticalBox(
      EntityInfoEnum, model.kind, model.kind = _)))
}

object OpenStoreUI extends EventCmdUI[OpenStore] {
  override def category = Windows
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

object PlayAnimationUI extends EventCmdUI[PlayAnimation] {
  override def category = Effects
  override def title = getMessage("Play_Animation")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: PlayAnimation) = {

    val fXOffset =
      new NumberSpinner(-2000, 2000, model.xOffset, model.xOffset = _)
    val fYOffset =
      new NumberSpinner(-2000, 2000, model.yOffset, model.yOffset = _)

    lazy val fOrigin = enumVerticalBox(Origins, model.originId, updateOriginId)
    lazy val fEntity = new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)

    def updateOriginId(newOriginId: Int): Unit = {
      model.originId = newOriginId
      val onEvent = newOriginId == Origins.ON_ENTITY.id
      fEntity.enabled = onEvent
      fXOffset.enabled = !onEvent
      fYOffset.enabled = !onEvent
    }
    updateOriginId(model.originId)

    Seq(
      EventField(getMessage("Animation"), indexedCombo(
        sm.getProjData.enums.animations, model.animationId,
        model.animationId = _)),
      EventField(getMessage("Animation_Origin"), fOrigin),
      EventField(getMessage("Entity"), fEntity),
      EventField(getMessage("X_Offset"), fXOffset),
      EventField(getMessage("Y_Offset"), fYOffset),
      EventField(getMessage("Animation_Speed"),
        percentField(0.25f, 4.0f, model.speedScale, model.speedScale = _)),
      EventField(getMessage("Size_Scale"),
        percentField(0.25f, 4.0f, model.sizeScale, model.sizeScale = _)))
  }
}

object PlayMusicUI extends EventCmdUI[PlayMusic] {
  override def category = Effects
  override def title = getMessage("Play_Music")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: PlayMusic) = Seq(
    EventField(
      getMessage("Music"),
      new MusicField(owner, sm, Some(model.spec), v => model.spec = v.get,
        allowNone = false)),
    EventField(
      "",
      boolField(getMessage("Loop"), model.loop, model.loop = _)),
    EventField(
      getMessage("Fade_Duration"),
      new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
        model.fadeDuration = _)))
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: PlayMusic) = List(
    IntNumberField(getMessage("Slot"), 0, MusicSlots.NUM_SLOTS,
      model.slot))
}

object PlaySoundUI extends EventCmdUI[PlaySound] {
  override def category = Effects
  override def title = getMessage("Play_Sound")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: PlaySound) = Seq(
    EventField(
      getMessage("Sound"),
      new SoundField(owner, sm, Some(model.spec), v => model.spec = v.get,
        allowNone = false)))
}

object CommentUI extends EventCmdUI[Comment] {
  override def category = Programming
  override def title = getMessage("Comment")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: Comment) = Seq(
    EventField("", {
      val textArea = new RSyntaxTextArea(20, 60)
      textArea.setText(model.commentString)
      textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE)
      textArea.setCodeFoldingEnabled(true)

      textArea.getDocument().addDocumentListener(new DocumentListener {
        override def changedUpdate(e: DocumentEvent) = Unit
        override def insertUpdate(e: DocumentEvent) =
          model.commentString = textArea.getText()
        override def removeUpdate(e: DocumentEvent) =
          model.commentString = textArea.getText()
      })

      val scrollPane = new RTextScrollPane(textArea)
      Component.wrap(scrollPane)
    }))
}

object RunJsUI extends EventCmdUI[RunJs] {
  override def category = Programming
  override def title = getMessage("Run_Javascript")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: RunJs) = Seq(
    EventField("", {
      val textArea = new RSyntaxTextArea(20, 60)
      textArea.setText(model.scriptBody)
      textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT)
      textArea.setCodeFoldingEnabled(true)

      textArea.getDocument().addDocumentListener(new DocumentListener {
        override def changedUpdate(e: DocumentEvent) = Unit
        override def insertUpdate(e: DocumentEvent) =
          model.scriptBody = textArea.getText()
        override def removeUpdate(e: DocumentEvent) =
          model.scriptBody = textArea.getText()
      })

      val scrollPane = new RTextScrollPane(textArea)
      Component.wrap(scrollPane)
    }))
}

object SetEventSpeedUI extends EventCmdUI[SetEventSpeed] {
  override def category = Movement
  override def title = getMessage("Set_Event_Speed")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: SetEventSpeed) = Seq(
    EventField("", new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)),
    EventField(getMessage("Speed"),
      new FloatSpinner(0.1f, 100f, 1f, model.speed, model.speed = _)))
}

object SetEventStateUI extends EventCmdUI[SetEventState] {
  override def category = Programming
  override def title = getMessage("Set_Event_State")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: SetEventState) = Seq(
    EventField("", new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = false, allowEventOnOtherMap = true)),
    EventField(getMessage("New_State"),
      new NumberSpinner(0, 127, model.state, model.state = _)))
}

object SetGlobalIntUI extends EventCmdUI[SetGlobalInt] {
  override def category = Programming
  override def title = getMessage("Set_Global_Integer")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: SetGlobalInt) = Seq(
    EventField(
      getMessage("Global_Variable_Name"),
      textField(model.key, model.key = _)),
    EventField(
      getMessage("Operation"),
      enumVerticalBox(
        OperatorType, model.operatorId, model.operatorId = _)))
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: SetGlobalInt) = List(
    IntNumberField(getMessage("Value"), -9999, 9999, model.value1))
}

object SetWindowskinUI extends EventCmdUI[SetWindowskin] {
  override def category = Windows
  override def title = getMessage("Set_Windowskin")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: SetWindowskin) = Seq(
    EventField(getMessage("Windowskin"), new WindowskinField(
      owner, sm, model.windowskinPath, model.windowskinPath = _)))
}

object ShowPictureUI extends EventCmdUI[ShowPicture] {
  override def category = Windows
  override def title = getMessage("Show_Picture")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: ShowPicture) = Seq(
    EventField(
      getMessage("Picture"),
      new PictureField(owner, sm, model.picture, model.picture = _)),
    EventField(
      getMessage("Layout"),
      new LayoutEditingPanel(model.layout)),
    EventField(
      getMessage("Alpha"),
      new FloatSpinner(0, 1f, 0.1f, model.alpha,
        model.alpha = _)))
  override def getParameterFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: ShowPicture) = List(
    IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
      PictureSlots.BATTLE_BEGIN - 1, model.slot))
}

object ShowTextUI extends EventCmdUI[ShowText] {
  override def category = Windows
  override def title = getMessage("Show_Text")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: ShowText) = {
    val fCharacter = indexedCombo(
      sm.getProjData.enums.characters,
      model.characterId,
      model.characterId = _)
    val fFace = new FaceField(owner, sm, model.customFace, model.customFace = _)

    def setEnabledFields() = {
      fCharacter.enabled = model.useCharacterFace
      fFace.enabled = !model.useCharacterFace
    }
    setEnabledFields()

    Seq(
      EventField(getMessage("Text"),
        textAreaField(model.lines, model.lines = _)),
      EventField(getMessage("Face"), fFace),
      EventField("", boolField(getMessage("Use_Character_Face"),
        model.useCharacterFace, model.useCharacterFace = _,
        Some(setEnabledFields))),
      EventField(getMessage("Character"), fCharacter))
  }
}

object StartBattleUI extends EventCmdUI[StartBattle] {
  override def category = Battles
  override def title = getMessage("Start_Battle")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: StartBattle) = Seq(
    EventField(
      getMessage("Encounter"),
      indexedCombo(sm.getProjData.enums.encounters, model.encounterId,
        model.encounterId = _)),
    EventField(
      getMessage("Override_Battle_Background"),
      new BattleBackgroundField(
        owner,
        sm,
        model.battleBackground,
        model.battleBackground = _)),
    EventField(
      getMessage("Override_Battle_Music"),
      new MusicField(owner, sm, model.battleMusic, model.battleMusic = _)))
}

object StopMusicUI extends EventCmdUI[StopMusic] {
  override def category = Effects
  override def title = getMessage("Stop_Music")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: StopMusic) = Seq(
    EventField(
      getMessage("Fade_Duration"),
      new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
        model.fadeDuration = _)))
  override def getParameterFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: StopMusic) = List(
    IntNumberField(getMessage("Slot"), 0, MusicSlots.NUM_SLOTS,
      model.slot))
}

object ChangeCameraFollowUI extends EventCmdUI[ChangeCameraFollow] {
  override def category = Movement
  override def title = needsTranslation("Change_Camera_Follow")
  override def getNormalFields(owner: Window, sm: StateMaster, mapName: Option[String] ,model: ChangeCameraFollow) = Seq(
          EventField("", new EntitySelectPanel(owner, sm, mapName,
      model.entitySpec, allowPlayer = true, allowEventOnOtherMap = false)))
}

object TeleportUI extends EventCmdUI[Teleport] {
  override def category = Movement
  override def title = getMessage("Teleport_Player")
  override def getNormalFields(owner: Window, sm: StateMaster,
    mapName: Option[String], model: Teleport) = Seq(
    EventField(getMessage("Transition"), enumVerticalBox(Transitions,
      model.transitionId, model.transitionId = _)),
    EventField(getMessage("Destination"),
      new MapLocPanel(owner, sm, model.loc, false)))
  override def newInstance(eventLoc: Option[MapLoc]) = {
    Teleport(eventLoc.getOrElse(MapLoc()))
  }
}

object TintScreenUI extends EventCmdUI[TintScreen] {
  override def category = Windows
  override def title = getMessage("Tint_Screen")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: TintScreen) = Seq(
    EventField(
      getMessageColon("Color_And_Alpha"),
      colorField(
        ColorSpec(model.r, model.g, model.b, model.a),
        v => {
          model.r = v.r
          model.g = v.g
          model.b = v.b
          model.a = v.a
        })),
    EventField(getMessageColon("Fade_Duration"),
      new FloatSpinner(
        0, 10f, 0.1f, model.fadeDuration, model.fadeDuration = _)))
}

object SleepUI extends EventCmdUI[Sleep] {
  override def category = Windows
  override def title = getMessage("Sleep")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: Sleep) = Seq(
    EventField(getMessage("Duration"),
      new FloatSpinner(
        0, 999999f, 0.1f, model.duration, model.duration = _)))
}

object MoveCameraUI extends EventCmdUI[MoveCamera] {
  override def category = Windows
  override def title = getMessage("Move_Camera")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: MoveCamera) = Seq(
    EventField(getMessage("X_Scroll_Value"),
      new FloatSpinner(
        -9999f, 9999f, 1f, model.dx, model.dx = _)),
    EventField(getMessage("Y_Scroll_Value"),
      new FloatSpinner(
        -9999f, 9999f, 1f, model.dy, model.dy = _)),
    EventField(getMessage("Duration"),
      new FloatSpinner(
        -9999f, 9999f, 0.1f, model.duration, model.duration = _)),
    EventField("", boolField(getMessage("Async"), model.async,
      model.async = _)))
}

object SetTransitionUI extends EventCmdUI[SetTransition] {

  override def category = Programming
  override def title = getMessage("Set_Transition")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: SetTransition) = Seq(
    EventField(getMessage("Transition_Type"), enumVerticalBox(
      Transitions, model.transitionId,
      model.transitionId = _)))
}

object StopSoundUI extends EventCmdUI[StopSound] {
  override def category = Effects
  override def title = getMessage("Stop_Sound")
}

object WhileLoopUI extends EventCmdUI[WhileLoop] {
  override def category = Programming
  override def title = getMessage("While_Loop")
  override def getNormalFields(
    owner: Window, sm: StateMaster, mapName: Option[String], model: WhileLoop) = Seq(
    EventField(getMessage("Conditions"),
      new ConditionsPanel(owner, sm.getProjData, model.conditions,
        model.conditions = _)))
}