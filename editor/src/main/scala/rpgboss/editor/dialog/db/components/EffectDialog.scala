package rpgboss.editor.dialog.db.components

import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.swing.AbstractButton
import scala.swing.Action
import scala.swing.BoxPanel
import scala.swing.ButtonGroup
import scala.swing.Component
import scala.swing.Dimension
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.RadioButton
import scala.swing.TabbedPane
import scala.swing.Window

import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.NumberSpinner
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils.indexedCombo
import rpgboss.model.AddStatusEffect
import rpgboss.model.Constants.MAXEFFECTARG
import rpgboss.model.Constants.MINEFFECTARG
import rpgboss.model.Effect
import rpgboss.model.EffectContext
import rpgboss.model.EscapeBattle
import rpgboss.model.HasName
import rpgboss.model.LearnSkill
import rpgboss.model.MetaEffect
import rpgboss.model.RecoverHpAdd
import rpgboss.model.RecoverHpMul
import rpgboss.model.RecoverMpAdd
import rpgboss.model.RecoverMpMul
import rpgboss.model.RemoveStatusEffect
import rpgboss.model.ResistElement
import rpgboss.model.UseSkill
import rpgboss.editor.Internationalized._

class EffectDialog(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: Effect,
  onOk: Effect => Unit,
  context: EffectContext.Value)
  extends StdDialog(owner, getMessage("Edit_Effect") {
  case class EffectControls(
    meta: MetaEffect,
    control: Component,
    getVal: () => Effect,
    setVal: (Effect) => Unit) {
    val btn: AbstractButton = new RadioButton() {
      action = Action(meta.name) {
        selectKey(meta.id)
      }
    }
  }

  centerDialog(new Dimension(400, 400))

  var model = initial
  var controls = Nil
  var selectedControls: EffectControls = null

  val helpLabel = new Label

  def selectKey(keyId: Int) = {
    selectedControls = effectsMap.get(keyId).get

    btnGroup.select(selectedControls.btn)

    effectsAll.foreach { eControls =>
      eControls.control.enabled = false
    }

    selectedControls.control.enabled = true

    val usability = Effect.getMeta(keyId).usability(context)

    helpLabel.foreground =
      if (usability.valid) java.awt.Color.BLACK else java.awt.Color.RED
    helpLabel.text = usability.helpMessage

    model = selectedControls.getVal()

    ctlPages.find(_.controls.exists(_.meta.id == keyId)).map(
        ctlPage => tabPane.selection.page = ctlPage.tabPage)
  }

  def onValueChange() = {
    model = selectedControls.getVal()
  }

  def nilEffect(meta: MetaEffect): EffectControls = {
    EffectControls(
      meta,
      new BoxPanel(Orientation.Vertical),
      () => Effect(meta.id),
      e => Unit)
  }

  def intEffect(meta: MetaEffect): EffectControls = {
    val spinner = new NumberSpinner(
      0,
      MINEFFECTARG,
      MAXEFFECTARG,
      onUpdate = v => onValueChange())

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("p") {
        preferredSize = new Dimension(15, 15)
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }

    EffectControls(
      meta,
      control,
      () => Effect(meta.id, spinner.getValue),
      e => spinner.setValue(e.v1))
  }

  def percentEffect(meta: MetaEffect): EffectControls = {
    val spinner = new NumberSpinner(
      0,
      -100,
      100,
      onUpdate = v => onValueChange())

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("%") {
        preferredSize = new Dimension(15, 15)
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }

    EffectControls(
      meta,
      control,
      () => Effect(meta.id, spinner.getValue),
      e => spinner.setValue(e.v1))
  }

  def choiceEffect[T <: HasName]
      (meta: MetaEffect, choices: Seq[T]): EffectControls = {
    val combo = indexedCombo(choices, 0, i => onValueChange())

    EffectControls(
      meta,
      combo,
      () => Effect(meta.id, combo.selection.index),
      e => combo.selection.index = e.v1)
  }

  def choiceWithValueEffect[T <% HasName](
    meta: MetaEffect,
    choices: Seq[T],
    initial: Int,
    min: Int,
    max: Int,
    label: String): EffectControls = {
    val combo = indexedCombo(choices, 0, i => onValueChange())
    val spinner = new NumberSpinner(
      initial,
      min,
      max,
      onUpdate = v => onValueChange()) {
      maximumSize = new Dimension(90, Int.MaxValue)
    }

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += combo
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += spinner
        contents += new Label(label) {
          preferredSize = new Dimension(15, 15)
        }
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        combo.enabled = b
        spinner.enabled = b
      }
    }

    EffectControls(
      meta,
      control,
      () => Effect(meta.id, combo.selection.index, spinner.getValue),
      e => {
        combo.selection.index = e.v1
        spinner.setValue(e.v2)
      })
  }

  def choicePercentEffect[T <% HasName]
      (meta: MetaEffect, choices: Seq[T]): EffectControls = {
    choiceWithValueEffect(meta, choices, 100, 0, 100, "%")
  }

  def choicePointsEffect[T <% HasName]
      (meta: MetaEffect, choices: Seq[T]): EffectControls = {
    choiceWithValueEffect(meta, choices, 0, MINEFFECTARG, MAXEFFECTARG, "p")
  }

  val effectsStatus = Array(
    intEffect(RecoverHpAdd),
    percentEffect(RecoverHpMul),
    intEffect(RecoverMpAdd),
    percentEffect(RecoverMpMul),
    choicePercentEffect(AddStatusEffect, dbDiag.model.enums.statusEffects),
    choicePercentEffect(RemoveStatusEffect, dbDiag.model.enums.statusEffects))

  val effectsStats = Array(
    choicePointsEffect(ResistElement, dbDiag.model.enums.elements))

  val effectsOther = Array(
    nilEffect(EscapeBattle),
    choiceEffect(UseSkill, dbDiag.model.enums.skills),
    choiceEffect(LearnSkill, dbDiag.model.enums.skills))

  val effectsAll = effectsStatus ++ effectsStats ++ effectsOther
  val effectsMap = Map(effectsAll.map(x => x.meta.id -> x): _*)

  val btnGroup = new ButtonGroup(effectsAll.map(_.btn): _*)

  class ControlPage(label: String, val controls: Seq[EffectControls]) {
    val panel = new DesignGridPanel {
      controls.foreach { eControls =>
        row()
          .grid().add(eControls.btn)
          .grid().add(eControls.control)
      }
    }
    val tabPage = new TabbedPane.Page(label, panel)
  }

  val ctlPages = Array(
    new ControlPage(getMessage("Status"), effectsStatus),
    new ControlPage(getMessage("Stats"), effectsStats),
    new ControlPage(getMessage("Other"), effectsOther))

  val tabPane = new TabbedPane {
    pages ++= ctlPages.map(_.tabPage)
  }

  // Does initialization of dialog
  {
    effectsMap.get(initial.keyId) map { ctrlGrp =>
      selectKey(ctrlGrp.meta.id)
      ctrlGrp.setVal(initial)
    }
  }

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    row().grid().add(helpLabel)

    addButtons(okBtn, cancelBtn)
  }

  def okFunc() = {
    onOk(model)
    close()
  }
}