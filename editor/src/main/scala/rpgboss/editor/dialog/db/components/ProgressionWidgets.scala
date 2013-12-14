package rpgboss.editor.dialog.db.components

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import rpgboss.model._
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.NumberSpinner

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

    def numSpinner(initial: Int, mutateF: (Int) => Unit) = {
      new NumberSpinner(initial, 0, 1000, onUpdate = { v =>
        mutateF(v)
        lvl50Val.text = model(50).toString
      })
    }

    new DesignGridPanel {
      border = BorderFactory.createTitledBorder(label)

      val mSpin = numSpinner(model.perLevel, model.perLevel = _)
      val bSpin = numSpinner(model.base, model.base = _)

      row().grid(leftLabel("L1 Base:")).add(bSpin)
      row().grid(leftLabel("Per level:")).add(mSpin)
      row().grid(leftLabel("At L50:")).add(lvl50Val)
    }
  }

  val fExp = progressionEditor("EXP to Level-Up", model.exp)
  val fMhp = progressionEditor("Max HP", model.mhp)
  val fMmp = progressionEditor("Max MP", model.mmp)
  val fAtk = progressionEditor("Attack", model.atk)
  val fSpd = progressionEditor("Speed", model.spd)
  val fMag = progressionEditor("Magic", model.mag)
  val fArm = progressionEditor("Base Armor", model.arm)
  val fMre = progressionEditor("Magic Resist", model.mre)

  row().grid().add(fMhp, fExp)
  row().grid().add(fMmp, fSpd)
  row().grid().add(fAtk, fArm)
  row().grid().add(fMag, fMre)
}