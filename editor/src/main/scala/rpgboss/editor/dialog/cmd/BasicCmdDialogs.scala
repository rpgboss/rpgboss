package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._

class StartBattleCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: StartBattle,
  successF: (StartBattle) => Any)
  extends StdDialog(owner, "StartBattle") {

  var encounterId = initial.encounterId 
  
  val encounterSelect = indexedCombo(
    sm.getProjData.enums.encounters,
    encounterId,
    encounterId = _)
    

  def okFunc() = {
    successF(StartBattle(encounterId))
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Encounter:"))
    row().grid().add(encounterSelect)

    addButtons(cancelBtn, okBtn)
  }
}