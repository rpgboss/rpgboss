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

class StatusPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Status Effects",
    dbDiag.model.enums.statusEffects)
  with DatabasePanel {
  def panelName = "Status Effects"
  def newDefaultInstance() = new StatusEffect()
  def label(item: StatusEffect) = item.name

  def editPaneForItem(idx: Int, model: StatusEffect) = {
    new BoxPanel(Orientation.Horizontal) {
      val leftPane = new DesignGridPanel {
        val fName = textField(model.name, model.name = _, 
                              Some(() => updatePreserveSelection(idx, model)))

        val fReleaseOnBattleEnd = boolField("", model.releaseOnBattleEnd,
                                            model.releaseOnBattleEnd = _)

        val fReleaseTime = new NumberSpinner(
          model.releaseTime,
          0,
          50,
          model.releaseTime = _)

        val fReleaseChance = new NumberSpinner(
          model.releaseChance,
          0,
          100,
          model.releaseChance = _)

        val fReleaseDmgChance = new NumberSpinner(
          model.releaseDmgChance,
          0,
          100,
          model.releaseDmgChance = _)

        val fMaxStacks = new NumberSpinner(
          model.maxStacks,
          0,
          50,
          model.maxStacks = _)

        row().grid(lbl("Name:")).add(fName)

        row().grid(lbl("State expiration in rounds:")).add(fReleaseTime)
        row().grid(lbl("Release chance after expiry:")).add(fReleaseChance)

        row().grid(lbl("Release after battle:")).add(fReleaseOnBattleEnd)

        row().grid(lbl("Maximum stacks:")).add(fMaxStacks)
      }

      val rightPane = 
          new EffectPanel(owner, dbDiag, model.effects, model.effects = _)

      contents += leftPane
      contents += rightPane
    }
  }

  override def onListDataUpdate() = {
    logger.info("Status effect data updated")
    dbDiag.model.enums.statusEffects = arrayBuffer
  }
}