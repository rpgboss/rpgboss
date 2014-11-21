package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.StateMaster
import rpgboss.lib.Utils
import rpgboss.model.AddOrRemove

class AddRemoveItemCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: AddRemoveItem,
  successF: (AddRemoveItem) => Any)
  extends StdDialog(owner, "Add/Remove Item") {

  centerDialog(new Dimension(200, 200))

  val model = Utils.deepCopy(initial)

  val fAddOrRemove = enumIdRadios(AddOrRemove)(
    AddOrRemove.fromBoolean(model.add).id,
    id => model.add = AddOrRemove.toBoolean(id))

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(
      new BoxPanel(Orientation.Horizontal) {
        addBtnsAsGrp(contents, fAddOrRemove)
      })
    ParameterFullComponent.addParameterFullComponentsToPanel(
        owner, sm.getProjData, this, model)

    addButtons(okBtn, cancelBtn)
  }
}