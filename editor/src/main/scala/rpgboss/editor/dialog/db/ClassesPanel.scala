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
import scala.collection.mutable.ArrayBuffer

class ClassesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Classes",
    dbDiag.model.enums.classes)
  with DatabasePanel {
  def panelName = "Classes"
  def newDefaultInstance() = new CharClass()

  def editPaneForItem(idx: Int, model: CharClass) = {
    val fName = textField(
      model.name,
      model.name = _,
      Some(refreshModel))
      
    val fUnarmedAttackSkillId = indexedCombo(
      dbDiag.model.enums.skills,
      model.unarmedAttackSkillId,
      model.unarmedAttackSkillId = _)

    val fEffects =
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _, true)

    logger.info("constructing new array multiselect panel %s".format(model.canUseItems) )
    
    val fCanEquip = new ArrayMultiselectPanel(
      owner,
      "Can equip",
      dbDiag.model.enums.items,
      model.canUseItems,
      model.canUseItems = _)
    
    val fLearnedSkills = new LearnedSkillPanel(
      owner, 
      dbDiag, 
      model.learnedSkills, 
      model.learnedSkills = _)

    val mainFields = new DesignGridPanel {
      row().grid(leftLabel("Name:")).add(fName)
      row().grid(leftLabel("Unarmed attack skill:")).add(fUnarmedAttackSkillId)
    }
    
    val rightFields = new GridPanel(2, 1) {
      contents += fCanEquip
      contents += fLearnedSkills
    }

    new BoxPanel(Orientation.Horizontal) {
      contents += mainFields
      contents += fEffects
      contents += rightFields
    }
  }

  override def onListDataUpdate() = {
    logger.info("Classes updated")
    dbDiag.model.enums.classes = dataAsArray
  }
}