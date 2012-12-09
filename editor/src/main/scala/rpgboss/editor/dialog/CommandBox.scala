package rpgboss.editor.dialog

import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.editor.dialog.cmd._
import rpgboss.editor.StateMaster

/**
 * Holds and edits list of commands.
 * Current state is held in listData member. 
 */
class CommandBox(
    evtDiag: EventDialog,
    owner: Window, 
    sm: StateMaster, 
    initialCmds: Array[EventCmd]) 
  extends ListView(initialCmds) {
  
  listenTo(mouse.clicks)
  
  def newCmdDialog() = {
    val d = new NewEvtCmdBox(evtDiag, sm, owner, this, selection.indices.head)
    d.open()
  }
  
  def editSelectedCmd() = {
    val selectedIdx = selection.indices.head
    val selectedCmd = selection.items.head
    val d = EventCmdDialog.dialogFor(owner, sm, selectedCmd, newEvt => {
      listData = listData.updated(selectedIdx, newEvt)
    })
    d.open()
  }
  
  val cmdBox = this
  reactions += {
    case MouseClicked(`cmdBox`, pt, _, clicks, _) =>
      if(clicks == 2) {
        selection.items.head match {
          case c: EndOfScript => newCmdDialog()
          case _ => editSelectedCmd()
        }
      }
  }
  
  def insertCmd(idx: Int, cmd: EventCmd) = {
    val newList = 
      listData.take(idx) ++ Seq(cmd) ++ listData.takeRight(listData.length-idx)
    
    listData = newList
  }
}