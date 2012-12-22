package rpgboss.editor.dialog

import rpgboss.editor.dialog.db._
import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class DatabaseDialog(owner: Window, sm: StateMaster) 
  extends StdDialog(owner, "Database")
{
  val sysPane  = new SystemPanel(owner, sm, sm.getProj.data)
  val enumPane = new EnumerationsPanel(owner, sm, sm.getProj.data) 
  val charPane = new CharactersPanel(owner, sm, sm.getProj.data)
  
  val panels = List(charPane, sysPane, enumPane)
  
  def applyFunc() = {
    
    // Apply every panel's updates to the project data using fold.
    // If anyone ever picks up this code, they will hate me.
    val newData = panels.foldLeft(sm.getProj.data) {
      (data, curPanel) => curPanel.updated(data)
    }
    
    val newProj = sm.getProj.copy(data = newData)
    
    sm.setProj(newProj)
  }
  
  def okFunc() = {
    applyFunc()
    close()
  }  
  
  val tabPane = new TabbedPane() {
    import TabbedPane._
    
    panels.foreach { panel =>
      pages += new Page(panel.panelName, panel)
    }
  }
  
  lazy val applyBtn = new Button(new Action("Apply") {
    def apply() = {
      applyFunc()
    }
  })
  
  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(cancelBtn, okBtn, Some(applyBtn))
  }

}