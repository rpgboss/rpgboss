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
  def panelName = getMessage("Enemies")
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

      val fLevel = new NumberSpinner(1, 9999, model.level, model.level = _)
      val fMhp = new NumberSpinner(5, 999999, model.mhp, model.mhp = _)
      val fMmp = new NumberSpinner(0, 99999, model.mmp, model.mmp = _)
      val fAtk = new NumberSpinner(1, 99999, model.atk, model.atk = _)
      val fSpd = new NumberSpinner(1, 100, model.spd, model.spd = _)
      val fMag = new NumberSpinner(1, 99999, model.mag, model.mag = _)
      val fArm = new NumberSpinner(1, 99999, model.arm, model.arm = _)
      val fMre = new NumberSpinner(1, 99999, model.mre, model.mre = _)

      val fExpValue =
        new NumberSpinner(0, 9999999, model.expValue, model.expValue = _)
      val fDroppedGold =
        new NumberSpinner(0, 9999999, model.droppedGold, model.droppedGold = _)

      val fAttackSkillId = indexedCombo(
          dbDiag.model.enums.skills,
          model.attackSkillId,
          model.attackSkillId = _)

      row().grid(leftLabel(getMessageColon("Name"))).add(fName)

      row().grid(leftLabel(getMessageColon("Battler"))).add(fBattler)

      row().grid(leftLabel(getMessageColon("Level"))).add(fLevel)

      row()
        .grid(leftLabel(getMessageColon("Max_HP"))).add(fMhp)
        .grid(leftLabel(getMessageColon("Max_MP"))).add(fMmp)

      row()
        .grid(leftLabel(getMessageColon("Attack"))).add(fAtk)
        .grid(leftLabel(getMessageColon("Magic"))).add(fMag)

      row()
        .grid(leftLabel(getMessageColon("Armor"))).add(fArm)
        .grid(leftLabel(getMessageColon("Magic_Resist"))).add(fMre)

      row()
        .grid(leftLabel(getMessageColon("Speed"))).add(fSpd)
        .grid()

      row().grid(leftLabel(getMessageColon("EXP_Value"))).add(fExpValue)
      row().grid(leftLabel(getMessageColon("Dropped_Gold"))).add(fDroppedGold)

      row().grid(leftLabel(getMessageColon("Attack_Skill"))).add(fAttackSkillId)
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
    logger.info(getMessage("Enemies_Data_Updated"))
    dbDiag.model.enums.enemies = dataAsArray
  }
}

class DroppedItemListPanel(
    owner: Window,
    projectData: ProjectData,
    initial: Array[ItemDrop],
    onUpdate: Array[ItemDrop] => Unit)
    extends TableEditor[ItemDrop] {
  override def title = getMessage("Dropped_Items")

  override val modelArray = ArrayBuffer(initial: _*)
  override def newInstance() = ItemDrop()

  override def onUpdate() = onUpdate(modelArray.toArray)

  override def colHeaders = Array(getMessage("Item"), getMessage("%_Chance"))

  override def getRowStrings(itemDrop: ItemDrop) = {
    Array(
      projectData.enums.items(itemDrop.itemId).name,
      Utils.floatToPercent(itemDrop.chance))
  }

  override def showEditDialog(
      initial: ItemDrop, okCallback: ItemDrop => Unit) = {
    val d = new StdDialog(owner, getMessage("ItemDrop")) {
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
        row().grid(lbl(getMessageColon("Item"))).add(fItemId)
        row().grid(lbl(getMessageColon("Chance"))).add(fChance)
        addButtons(okBtn, cancelBtn)
      }
    }
    d.open()
  }
}
