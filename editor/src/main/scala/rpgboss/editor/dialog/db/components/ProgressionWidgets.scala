package rpgboss.editor.dialog.db.components

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import rpgboss.model._
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.FloatSpinner
import rpgboss.editor.Internationalized._

/**
 * Edits the model in-place.
 */
class StatProgressionPanel(model: StatProgressions)
  extends DesignGridPanel {

  def progressionEditor(label: String, model: Curve) = {
    val lvl50Val = new TextField {
      editable = false
      text = model(50).toString
    }

    def numSpinner(initial: Float, mutateF: (Float) => Unit) = {
      new FloatSpinner(initial, 0, 1000, onUpdate = { v =>
        mutateF(v)
        lvl50Val.text = model(50).toString
      },
      step = 0.1f)
    }

    new DesignGridPanel {
      border = BorderFactory.createTitledBorder(label)

      val mSpin = numSpinner(model.perLevel, model.perLevel = _)
      val bSpin = numSpinner(model.base, model.base = _)

      row().grid(leftLabel(getMessage("L1_Base") + ":")).add(bSpin)
      row().grid(leftLabel(getMessage("Per_level") + ":")).add(mSpin)
      row().grid(leftLabel(getMessage("At_L50") + ":")).add(lvl50Val)
    }
  }

  val fExp = progressionEditor(getMessage("EXP_To_Level-Up"), model.exp)
  val fMhp = progressionEditor(getMessage("Max HP"), model.mhp)
  val fMmp = progressionEditor(getMessage("Max MP"), model.mmp)
  val fAtk = progressionEditor(getMessage("Attack"), model.atk)
  val fSpd = progressionEditor(getMessage("Speed"), model.spd)
  val fMag = progressionEditor(getMessage("Magic"), model.mag)
  val fArm = progressionEditor(getMessage("Base_Armor"), model.arm)
  val fMre = progressionEditor(getMessage("Magic_Resist"), model.mre)

  row().grid().add(fMhp, fExp)
  row().grid().add(fMmp, fSpd)
  row().grid().add(fAtk, fArm)
  row().grid().add(fMag, fMre)
}