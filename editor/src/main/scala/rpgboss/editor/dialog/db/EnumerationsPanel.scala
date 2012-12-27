package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog

class EnumerationsPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends DesignGridPanel 
  with DatabasePanel
{
  def panelName = "Enumerations"
  layout.labelAlignment(LabelAlignment.RIGHT)
  
  val fDamageTypes =
    new StringArrayEditingPanel(
        owner,
        "Damage types",
        dbDiag.model.damageTypes) {
    
    override def onListDataUpdate() = {
      logger.info("Damage types updated")
      dbDiag.model = dbDiag.model.copy(
          damageTypes = array
      )
    }
  }
  
  val fSkillTypes = 
    new StringArrayEditingPanel(
        owner,
        "Skill types",
        dbDiag.model.skillTypes) {
    override def onListDataUpdate() = {
      logger.info("Skill types updated")
      dbDiag.model = dbDiag.model.copy(
          skillTypes = array
      )
    }
  }
  
  row.grid().add(fDamageTypes).add(fSkillTypes)
}