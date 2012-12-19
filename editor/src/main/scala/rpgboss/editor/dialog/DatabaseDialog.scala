package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class SystemPanel(
    owner: Window, 
    sm: StateMaster, 
    initial: ProjectData) 
  extends DesignGridPanel 
{
  val fGameTitle = new TextField() {
    text = initial.title
  }
  row().grid(lbl("Game title:")).add(fGameTitle)
  
  val fWindowskin = new WindowskinField(owner, sm, initial.windowskin)
  row().grid(lbl("Windowskin:")).add(fWindowskin)
}

class DatabaseDialog(owner: Window, sm: StateMaster) 
  extends StdDialog(owner, "Database")
{
  def applyFunc() = {
    
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