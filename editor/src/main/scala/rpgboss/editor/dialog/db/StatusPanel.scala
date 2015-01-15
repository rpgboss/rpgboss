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
import rpgboss.editor.Internationalized._

class StatusPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.statusEffects)
  with DatabasePanel {
  def panelName = getMessage("Status_Effects")
  def newDefaultInstance() = new StatusEffect()

  def editPaneForItem(idx: Int, model: StatusEffect) = {
    new BoxPanel(Orientation.Horizontal) with DisposableComponent {
      val leftPane = new DesignGridPanel {
        val fName = textField(model.name, model.name = _,
                              Some(() => updatePreserveSelection(idx, model)))

        val fReleaseChancePerTick = percentIntField(
          0,
          100,
          model.releaseChancePerTick,
          model.releaseChancePerTick = _)

        val fMaxStacks = new NumberSpinner(
          0,
          50,
          model.maxStacks,
          model.maxStacks = _)

        row().grid(lbl(getMessageColon("Name"))).add(fName)

        row().grid(lbl(needsTranslation("Release chance per tick:")))
          .add(fReleaseChancePerTick)

        row().grid(lbl(getMessageColon("Maximum_Stacks"))).add(fMaxStacks)
      }

      val rightPane =
          new EffectPanel(owner, dbDiag, model.effects, model.effects = _,
              EffectContext.StatusEffect)

      contents += leftPane
      contents += rightPane
    }
  }

  override def onListDataUpdate() = {
    logger.info(getMessage("Status_Effect_Data_Updated"))
    dbDiag.model.enums.statusEffects = dataAsArray
  }
}