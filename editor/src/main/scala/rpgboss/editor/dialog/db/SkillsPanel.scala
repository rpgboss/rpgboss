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
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._

class SkillsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Skills",
    dbDiag.model.enums.skills) {
  def panelName = "Skills"
  def newDefaultInstance() = Skill()
  def label(a: Skill) = a.name

  def editPaneForItem(idx: Int, model: Skill) = {
    new DesignGridPanel {
      val fName = textField(
        model.name,
        v => {
          model.name = v
          refreshModel()
        })
      
      val fScope = enumCombo(Scope)(
        model.scopeId,
        v => model.scopeId = v.id)
      
      val fDamages = new DamagesPanel(dbDiag, model.damages, model.damages = _)
        
      row().grid(lbl("Name:")).add(fName)
      row().grid(lbl("Scope:")).add(fScope)
      row().grid(lbl("Damages:")).add(fDamages)
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.skills = arrayBuffer
  }
}