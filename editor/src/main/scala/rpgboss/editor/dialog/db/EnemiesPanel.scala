package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._

class EnemiesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Enemies",
    dbDiag.model.enums.enemies) {
  def panelName = "Enemies"
  def newDefaultInstance() = new Enemy()
  def label(e: Enemy) = e.name

  def editPaneForItem(idx: Int, model: Enemy) = {
    val bioFields = new DesignGridPanel {
      val fName = textField(
        model.name,
        v => {
          model.name = v
          refreshModel()
        })

      val fBattler = new BattlerField(
        owner,
        sm,
        model.battler,
        model.battler = _)

      val fLevel = new NumberSpinner(model.level, 1, 50, model.level = _)
      val fMhp = new NumberSpinner(model.mhp, 5, 5000, model.mhp = _)
      val fMmp = new NumberSpinner(model.mmp, 0, 500, model.mhp = _)
      val fAtk = new NumberSpinner(model.atk, 1, 50, model.atk = _)
      val fSpd = new NumberSpinner(model.spd, 1, 50, model.spd = _)
      val fMag = new NumberSpinner(model.mag, 1, 50, model.mag = _)
      val fArm = new NumberSpinner(model.arm, 1, 50, model.arm = _)
      val fMre = new NumberSpinner(model.mre, 1, 50, model.mre = _)

      val fExpValue =
        new NumberSpinner(model.expValue, 10, 50000, model.expValue = _)

      row().grid(leftLabel("Name:")).add(fName)

      row().grid(leftLabel("Battler:")).add(fBattler)

      row().grid(leftLabel("Level:")).add(fLevel)

      row().grid(leftLabel("Max HP:")).add(fMhp)
      row().grid(leftLabel("Max MP:")).add(fMmp)

      row().grid(leftLabel("Attack:")).add(fAtk)
      row().grid(leftLabel("Magic:")).add(fMag)

      row().grid(leftLabel("Armor:")).add(fArm)
      row().grid(leftLabel("Magic Resist:")).add(fMre)

      row().grid(leftLabel("Speed:")).add(fSpd)
      row().grid(leftLabel("EXP Value:")).add(fExpValue)
    }

    val fEffects =
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _, false)

    new BoxPanel(Orientation.Horizontal) {
      contents += bioFields
      contents += fEffects
    }
  }

  override def onListDataUpdate() = {
    logger.info("Enemies data updated")
    dbDiag.model.enums.enemies = arrayBuffer
  }
}