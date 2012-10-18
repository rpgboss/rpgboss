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
  
  
}