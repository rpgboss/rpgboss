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

  val fItemId = new IntParameterEnumerationIndexField(
      owner, model.itemId, sm.getProj.data.enums.items)

  val fAddOrRemove = enumIdRadios(AddOrRemove)(
    AddOrRemove.fromBoolean(model.add).id,
    id => model.add = AddOrRemove.toBoolean(id))

  val fQty = new IntParameterNumberField(owner, model.qty, 1, 99)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(
      new BoxPanel(Orientation.Horizontal) {
        addBtnsAsGrp(contents, fAddOrRemove)
      })
    row().grid(lbl("Item:")).add(fItemId)
    row().grid(lbl("Quantity:")).add(fQty)

    addButtons(cancelBtn, okBtn)
  }
}