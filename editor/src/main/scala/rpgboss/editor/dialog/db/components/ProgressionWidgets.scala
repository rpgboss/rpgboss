package rpgboss.editor.dialog.db.components

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import rpgboss.model._
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.NumberSpinner

class CharProgressionPanel(
  initial: CharProgressions,
  onUpdate: (CharProgressions) => Unit)
  extends DesignGridPanel {
  //border = BorderFactory.createTitledBorder("Stat progressions")

  var model = initial
  def updateModel(newModel: CharProgressions) = {
    model = newModel
    onUpdate(model)
  }

  def progressionEditor(
    label: String,
    initial: Curve,
    onUpdate: (Curve) => Unit) =
    {
      var model = initial

      val lvl50Val = new TextField {
        editable = false
        text = model(50).toString
      }

      def numSpinner(initial: Int, mutateF: (Int) => Unit) = {
        new NumberSpinner(initial, 0, 1000, onUpdate = { v =>
          mutateF(v)
          onUpdate(model)

          lvl50Val.text = model(50).toString
        })
      }

      new DesignGridPanel {
        border = BorderFactory.createTitledBorder(label)

        val mSpin = numSpinner(initial.perLevel, 
                               v => model = model.copy(perLevel = v))
        val bSpin = numSpinner(initial.base, 
                               v => model = model.copy(base = v))

        row().grid(leftLabel("L1 Base:")).add(bSpin)
        row().grid(leftLabel("Per level:")).add(mSpin)
        row().grid(leftLabel("At L50:")).add(lvl50Val)
      }
    }

  val fExp = progressionEditor(
    "EXP to Level-Up", model.exp, p => updateModel(model.copy(exp = p)))
  val fMhp = progressionEditor(
    "Max HP", model.mhp, p => updateModel(model.copy(mhp = p)))
  val fMmp = progressionEditor(
    "Max MP", model.mmp, p => updateModel(model.copy(mmp = p)))
  val fAtk = progressionEditor(
    "Attack", model.atk, p => updateModel(model.copy(atk = p)))
  val fSpd = progressionEditor(
    "Speed", model.spd, p => updateModel(model.copy(spd = p)))
  val fMag = progressionEditor(
    "Magic", model.mag, p => updateModel(model.copy(mag = p)))

  row().grid().add(fMhp, fAtk)
  row().grid().add(fMmp, fSpd)
  row().grid().add(fExp, fMag)
}