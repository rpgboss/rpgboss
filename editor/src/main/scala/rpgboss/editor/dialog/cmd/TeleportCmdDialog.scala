package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.model.Constants._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog.StdDialog
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.editor.lib.MapLocPanel
import rpgboss.editor.StateMaster

class TeleportCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: Teleport,
  successF: (Teleport) => Any)
  extends StdDialog(owner, "Teleport Player") {

  def okFunc() = {
    val cmd = Teleport(mapLocPanel.loc, transition)
    successF(cmd)
    close()
  }

  var transition = initial.transition

  val transitionRadios = new BoxPanel(Orientation.Horizontal) {
    val radioBtns = enumRadios(Transitions)(
      Transitions(transition),
      t => transition = t.id)

    addBtnsAsGrp(contents, radioBtns)
  }

  val mapLocPanel = new MapLocPanel(owner, sm, initial.loc)

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Transition:"))
    row().grid().add(transitionRadios)
    row().grid().add(leftLabel("Destination:"))
    row().grid().add(mapLocPanel)
    addButtons(cancelBtn, okBtn)
  }

}

object MetadataMode extends Enumeration {
  type MetadataMode = Value
  val Passability, Height = Value
}
