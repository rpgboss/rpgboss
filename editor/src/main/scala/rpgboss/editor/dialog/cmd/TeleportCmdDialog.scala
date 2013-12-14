package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.model.Constants._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.misc.MapLocPanel
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
    val radioBtns = enumIdRadios(Transitions)(transition, transition = _)

    addBtnsAsGrp(contents, radioBtns)
  }

  val mapLocPanel = new MapLocPanel(owner, sm, initial.loc, false)

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
