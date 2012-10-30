package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog.cmd._
import rpgboss.model.event._

class NewEvtCmdBox(
    owner: Window,
    cmdBox: CommandBox,
    idxToInsert: Int) 
  extends StdDialog(owner, "New command") 
{

  // Noop, as there is no okay button
  def okFunc() = {}
  
  def btnEvtCmd(title: String, e: EventCmd) = {
    new Button() {
      action = Action(title) {
        val d = EventCmdDialog.dialogFor(
        owner,
        e,
        evtCmd => {
          NewEvtCmdBox.this.close()
          cmdBox.insertCmd(idxToInsert, evtCmd)
        })
        d.open()
      }
    }
  }
  
  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Windows:"))
    row().grid().add(btnEvtCmd("Show text...", ShowText()))
    
    addCancel(cancelBtn)
  }

}