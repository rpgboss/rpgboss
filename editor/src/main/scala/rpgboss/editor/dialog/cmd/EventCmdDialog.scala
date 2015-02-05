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
import rpgboss.editor.dialog.cmd.EventCmdCategory.Audio
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
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.EntitySelectPanel
import rpgboss.editor.uibase.EventParameterField
import rpgboss.editor.uibase.EventParameterField.FloatPercentField
import rpgboss.editor.uibase.EventParameterField.IntEnumIdField
import rpgboss.editor.uibase.EventParameterField.IntMultiselectField
import rpgboss.editor.uibase.EventParameterField.IntNumberField
import rpgboss.editor.uibase.FloatSpinner
import rpgboss.editor.uibase.LayoutEditingPanel
import rpgboss.editor.uibase.NumberSpinner
import rpgboss.editor.uibase.ParameterFullComponent
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.StringArrayEditingPanel
import rpgboss.editor.uibase.SwingUtils.boolEnumHorizBox
import rpgboss.editor.uibase.SwingUtils.boolField
import rpgboss.editor.uibase.SwingUtils.colorField
import rpgboss.editor.uibase.SwingUtils.enumVerticalBox
import rpgboss.editor.uibase.SwingUtils.indexedCombo
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.editor.uibase.SwingUtils.percentField
import rpgboss.editor.uibase.SwingUtils.textAreaField
import rpgboss.editor.uibase.SwingUtils.textField
import rpgboss.lib.ArrayUtils
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove
import rpgboss.model.HealOrDamageEnum
import rpgboss.model.PictureSlots
import rpgboss.model.RpgMapData
import rpgboss.model.Transitions
import rpgboss.model.event.AddRemoveGold
import rpgboss.model.event.AddRemoveItem
import rpgboss.model.event.EventCmd
import rpgboss.model.event.GetChoice
import rpgboss.model.event.HealOrDamage
import rpgboss.model.event.HidePicture
import rpgboss.model.event.IfCondition
import rpgboss.model.event.ModifyParty
import rpgboss.model.event.MoveEvent
import rpgboss.model.event.OpenStore
import rpgboss.model.event.OperatorType
import rpgboss.model.event.PlayMusic
import rpgboss.model.event.PlaySound
import rpgboss.model.event.RunJs
import rpgboss.model.event.SetEventState
import rpgboss.model.event.SetGlobalInt
import rpgboss.model.event.ShowPicture
import rpgboss.model.event.ShowText
import rpgboss.model.event.StartBattle
import rpgboss.model.event.StopMusic
import rpgboss.model.event.Teleport
import rpgboss.model.event.TintScreen
import rpgboss.model.event.WhileLoop
import rpgboss.player.RpgScreen
import rpgboss.model.MapLoc

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
  val eventCmdUis: Seq[EventCmdUI[_]] = List(
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

  def category: EventCmdCategory.Value
  def title: String
  def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String],
      model: EventCmdType): Seq[TitledComponent] = Nil
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
      initial: EventCmdType, successF: EventCmdType => Any) =
    // ownerArg is named differently from owner, to prevent EventCmdDialog.owner
    // (which is still null), from shadowing the non-null function argument
    new EventCmdDialog(ownerArg, sm, title, initial, successF) {
      override def normalFields =
        EventCmdUI.this.getNormalFields(ownerArg, sm, mapName, model)
      override def parameterFields =
        EventCmdUI.this.getParameterFields(ownerArg, sm, mapName, model)
    }
}

object AddRemoveItemUI extends EventCmdUI[AddRemoveItem] {
  override def category = Inventory
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
  override def category = Inventory
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
  override def category = Windows
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
  override def category = Party
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
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)),
    TitledComponent("", boolField(getMessage("ELSE_Branch"),
        model.elseBranch, model.elseBranch = _)))
}

object ModifyPartyUI extends EventCmdUI[ModifyParty] {
  override def category = Party
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
  override def category = Movement
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

object PlayMusicUI extends EventCmdUI[PlayMusic] {
  override def category = Audio
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
  override def category = Audio
  override def title = getMessage("Play_Sound")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: PlaySound) = Seq(
    TitledComponent(
        getMessage("Sound"),
        new SoundField(owner, sm, Some(model.spec), v => model.spec = v.get,
            allowNone = false)))
}

object RunJsUI extends EventCmdUI[RunJs] {
  override def category = Programming
  override def title = getMessage("Run_Javascript")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: RunJs) = Seq(
    TitledComponent("", {
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

object SetEventStateUI extends EventCmdUI[SetEventState] {
  override def category = Programming
  override def title = getMessage("Set_Event_State")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: SetEventState) = Seq(
    TitledComponent("", new EntitySelectPanel(owner, sm, mapName,
        model.entitySpec, allowPlayer = false, allowEventOnOtherMap = true)),
    TitledComponent(getMessage("New_State"),
        new NumberSpinner(0, 127, model.state, model.state = _)))
}

object SetGlobalIntUI extends EventCmdUI[SetGlobalInt] {
  override def category = Programming
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
  override def category = Windows
  override def title = getMessage("Show_Picture")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: ShowPicture) = Seq(
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

object ShowTextUI extends EventCmdUI[ShowText] {
  override def category = Windows
  override def title = getMessage("Show_Text")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: ShowText) = Seq(
    TitledComponent(getMessage("Text"),
        textAreaField(model.lines, model.lines = _)))
}

object StartBattleUI extends EventCmdUI[StartBattle] {
  override def category = Battles
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
  override def category = Audio
  override def title = getMessage("Stop_Music")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: StopMusic) = Seq(
    TitledComponent(
        getMessage("Fade_Duration"),
        new FloatSpinner(0, 10f, 0.1f, model.fadeDuration,
            model.fadeDuration = _)))
  override def getParameterFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: StopMusic) = List(
    IntNumberField(getMessage("Slot"), 0, RpgScreen.MAX_MUSIC_SLOTS,
        model.slot))
}

object TeleportUI extends EventCmdUI[Teleport] {
  override def category = Movement
  override def title = getMessage("Teleport_Player")
  override def getNormalFields(owner: Window, sm: StateMaster,
      mapName: Option[String], model: Teleport) = Seq(
    TitledComponent(getMessage("Transition"), enumVerticalBox(Transitions,
        model.transitionId, model.transitionId = _)),
    TitledComponent(getMessage("Destination"),
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
  override def category = Programming
  override def title = getMessage("While_Loop")
  override def getNormalFields(
      owner: Window, sm: StateMaster, mapName: Option[String], model: WhileLoop) = Seq(
    TitledComponent(getMessage("Conditions"),
        new ConditionsPanel(owner, sm.getProjData, model.conditions,
            model.conditions = _)))
}
