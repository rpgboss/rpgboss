package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._
import rpgboss.editor.resourceselector.BattleBackgroundField
import rpgboss.lib.Utils

class StartBattleCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: StartBattle,
  successF: (StartBattle) => Any)
  extends StdDialog(owner, "StartBattle") {

  var model = Utils.deepCopy(initial)

  val encounterSelect = indexedCombo(
    sm.getProjData.enums.encounters,
    model.encounterId,
    v => model = model.copy(encounterId = v))

  val battleBgSelect = new BattleBackgroundField(
    owner,
    sm,
    model.battleBackground,
    v => model = model.copy(battleBackground = v))

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Encounter:"))
    row().grid().add(encounterSelect)

    row().grid().add(leftLabel("Battle Background:"))
    row().grid().add(battleBgSelect)

    addButtons(cancelBtn, okBtn)
  }
}