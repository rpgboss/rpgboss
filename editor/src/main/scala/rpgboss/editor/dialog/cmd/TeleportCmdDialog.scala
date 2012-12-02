package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.model.Constants._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog.StdDialog
import rpgboss.editor.lib.DesignGridPanel

class TeleportCmdDialog(
    owner: Window, 
    initial: Teleport, 
    successF: (Teleport) => Any) 
  extends StdDialog (owner, "Teleport Player")
{
  
  def okFunc() = {
    successF(initial)
    close()
  }
  
  var transition = initial.transition
  
  val transitionRadios = new BoxPanel(Orientation.Horizontal) {
    val radioBtns = enumRadios(Transitions)(
        Transitions(transition),
        t => transition = t.id)
    
    addBtnsAsGrp(contents, radioBtns)
  }
  
  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Transition:"))
    row().grid().add(transitionRadios)
    addButtons(cancelBtn, okBtn)
  }
  
}

object MetadataMode extends Enumeration 
{
  type MetadataMode = Value
  val Passability, Height = Value
}
