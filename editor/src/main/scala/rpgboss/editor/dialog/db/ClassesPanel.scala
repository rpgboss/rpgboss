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
  def label(charClass: CharClass) = charClass.name
  def copyItem(c: CharClass) = c.copy()

  def editPaneForItem(idx: Int, model: CharClass) = {
    val fName = textField(
      model.name,
      v => {
        model.name = v
        refreshModel()
      })

    val fEffects =
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _, true)

    val fCanEquip = new ArrayMultiselectPanel(
      owner,
      "Can equip",
      dbDiag.model.enums.items,
      model.canUseItems,
      v => model.canUseItems = ArrayBuffer(v :_*))
    
    val fLearnedSkills = new LearnedSkillPanel(
      owner, 
      dbDiag, 
      model.learnedSkills, 
      model.learnedSkills = _)

    val mainFields = new DesignGridPanel {

      row().grid(leftLabel("Name:")).add(fName)

    }

    new BoxPanel(Orientation.Horizontal) {
      contents += mainFields
      contents += fCanEquip
      contents += fEffects
      contents += fLearnedSkills
    }
  }

  override def onListDataUpdate() = {
    logger.info("Classes updated")
    dbDiag.model.enums.classes = arrayBuffer
  }
}