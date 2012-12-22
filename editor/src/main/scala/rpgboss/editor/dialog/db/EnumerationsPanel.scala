package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class EnumerationsPanel(
    owner: Window, 
    sm: StateMaster, 
    initial: ProjectData) 
  extends DesignGridPanel 
  with DatabasePanel
{
  def panelName = "Enumerations"
  layout.labelAlignment(LabelAlignment.RIGHT)
  
  val fDamageTypes =
    new StringArrayEditingPanel(
        owner,
        "Damage types",
        initial.damageTypes)
  
  val fSkillTypes = 
    new StringArrayEditingPanel(
        owner,
        "Skill types",
        initial.skillTypes)
  
  row.grid().add(fDamageTypes).add(fSkillTypes)
  
  def updated(data: ProjectData) = {
    data.copy(
        damageTypes = fDamageTypes.array,
        skillTypes  = fSkillTypes.array
    )
  }
}