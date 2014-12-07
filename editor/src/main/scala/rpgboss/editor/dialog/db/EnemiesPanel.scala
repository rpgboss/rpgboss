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
import rpgboss.editor.misc.RandomEncounterSettingsPanel
import scala.collection.mutable.ArrayBuffer
import rpgboss.lib.Utils
import rpgboss.editor.Internationalized._


class EnemiesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.enemies) {
  def panelName = "Enemies"
  def newDefaultInstance() = new Enemy()

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
      val fDroppedGold =
        new NumberSpinner(model.droppedGold, 0, 9999, model.droppedGold = _)

      val fAttackSkillId = indexedCombo(
          dbDiag.model.enums.skills,
          model.attackSkillId,
          model.attackSkillId = _)

      row().grid(leftLabel(getMessage("Name") + ":")).add(fName)

      row().grid(leftLabel(getMessage("Battler") + ":")).add(fBattler)

      row().grid(leftLabel(getMessage("Level") + ":")).add(fLevel)

      row().grid(leftLabel(getMessage("Max_HP") + ":")).add(fMhp)
      row().grid(leftLabel(getMessage("Max_MP") + ":")).add(fMmp)

      row().grid(leftLabel(getMessage("Attack") + ":")).add(fAtk)
      row().grid(leftLabel(getMessage("Magic") + ":")).add(fMag)

      row().grid(leftLabel(getMessage("Armor") + ":")).add(fArm)
      row().grid(leftLabel(getMessage("Magic_Resist") + ":")).add(fMre)

      row().grid(leftLabel(getMessage("Speed") + ":")).add(fSpd)
      row().grid(leftLabel(getMessage("EXP_Value") + ":")).add(fExpValue)
      row().grid(leftLabel(getMessage("Dropped_Gold") + ":")).add(fDroppedGold)

      row().grid(leftLabel(getMessage("Attack_Skill") + ":")).add(fAttackSkillId)
    }

    val fItemDrops = new DroppedItemListPanel(
        owner, dbDiag.model, model.droppedItems, model.droppedItems = _)

    val fEffects =
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _,
          EffectContext.Enemy)

    val fSkills = new ArrayMultiselectPanel(
      owner,
      "Known Skills",
      dbDiag.model.enums.skills,
      model.skillIds,
      model.skillIds = _)

    new BoxPanel(Orientation.Horizontal) with DisposableComponent {
      contents += bioFields
      contents += new BoxPanel(Orientation.Vertical) {
        contents += fItemDrops
        contents += fEffects
        contents += fSkills
      }
    }
  }

  override def onListDataUpdate() = {
    logger.info("Enemies data updated")
    dbDiag.model.enums.enemies = dataAsArray
  }
}

class DroppedItemListPanel(
    owner: Window,
    projectData: ProjectData,
    initial: Array[ItemDrop],
    onUpdate: Array[ItemDrop] => Unit)
    extends TableEditor[ItemDrop] {
  override def title = "Dropped Items"

  override val modelArray = ArrayBuffer(initial: _*)
  override def newInstance() = ItemDrop()

  override def onUpdate() = onUpdate(modelArray.toArray)

  override def colHeaders = Array("Item", "% Chance")

  override def getRowStrings(itemDrop: ItemDrop) = {
    Array(
      projectData.enums.items(itemDrop.itemId).name,
      Utils.floatToPercent(itemDrop.chance))
  }

  override def showEditDialog(
      initial: ItemDrop, okCallback: ItemDrop => Unit) = {
    val d = new StdDialog(owner, "ItemDrop") {
      val model = initial.copy()
      def okFunc() = {
        okCallback(model)
        close()
      }

      val fItemId = indexedCombo(
          projectData.enums.items,
          model.itemId,
          model.itemId = _)

      val fChance = percentField(0, 1, model.chance, model.chance = _)

      contents = new DesignGridPanel {
        row().grid(lbl("Item:")).add(fItemId)
        row().grid(lbl("Chance:")).add(fChance)
        addButtons(okBtn, cancelBtn)
      }
    }
    d.open()
  }
}