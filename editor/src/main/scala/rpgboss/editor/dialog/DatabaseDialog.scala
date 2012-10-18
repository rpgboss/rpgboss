package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class DatabaseDialog(owner: Window, sm: StateMaster) 
  extends StdDialog(owner, "Database")
{
  def okFunc() = {
    close()
  }
  
  val tabPane = new TabbedPane() {
    import TabbedPane._
    pages += new Page("Party", new BoxPanel(Orientation.Vertical))
    pages += new Page("System", new BoxPanel(Orientation.Vertical))
  }
  
  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(cancelButton, okButton)
  }

}