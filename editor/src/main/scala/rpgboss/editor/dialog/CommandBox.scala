package rpgboss.editor.dialog

import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.event._

/**
 * Holds and edits list of commands.
 * Current state is held in listData member. 
 */
class CommandBox(
    owner: Window, 
    project: Project, 
    initialCmds: Array[EventCmd],
    onUpdate: (Array[EventCmd]) => Any) 
  extends ListView(initialCmds) {
  
  listenTo(mouse.clicks)
  
  val cmdBox = this
  reactions += {
    case MouseClicked(`cmdBox`, pt, _, clicks, _) =>
      if(clicks == 2) {
        if(!selection.indices.isEmpty) {
          val idx = selection.indices.head
          val d = new NewEvtCmdBox(owner, this, idx)
          d.open()
        }
      }
  }
  
  def insertCmd(idx: Int, cmd: EventCmd) = {
    val newList = 
      listData.take(idx) ++ Seq(cmd) ++ listData.takeRight(listData.length-idx)
    
    listData = newList
    
    onUpdate(listData.toArray)
  }
}