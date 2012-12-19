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
  def applyFunc() = {
    val newData = 
      sysPane.updated(sm.getProj.data)
    
    val newProj = sm.getProj.copy(data = newData)
    
    sm.setProj(newProj)
  }
  
  def okFunc() = {
    applyFunc()
    close()
  }
  
  val sysPane = new SystemPanel(owner, sm, sm.getProj.data)
  
  val tabPane = new TabbedPane() {
    import TabbedPane._
    pages += new Page("Party", new BoxPanel(Orientation.Vertical))
    pages += new Page("System", sysPane)
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