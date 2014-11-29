package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor._
import rpgboss.editor.resourceselector.BattleBackgroundField
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove

class StartBattleCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: StartBattle,
  successF: (StartBattle) => Any)
  extends StdDialog(owner, "StartBattle") {

  centerDialog(new Dimension(300, 200))

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

    addButtons(okBtn, cancelBtn)
  }
}

class AddRemoveItemCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: AddRemoveItem,
  successF: (AddRemoveItem) => Any)
  extends EventCmdDialog(owner, sm, "Add/Remove Item", initial, successF) {

  override def extraFields = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add, model.add = _))
  )
}

class AddRemoveGoldCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: AddRemoveGold,
  successF: (AddRemoveGold) => Any)
  extends EventCmdDialog(owner, sm, "Add/Remove Item", initial, successF) {

  override def extraFields = Seq(
    TitledComponent("", boolEnumHorizBox(AddOrRemove, model.add, model.add = _))
  )
}

class OpenStoreCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: OpenStore,
  successF: (OpenStore) => Any)
  extends EventCmdDialog(owner, sm, "Open Store", initial, successF)