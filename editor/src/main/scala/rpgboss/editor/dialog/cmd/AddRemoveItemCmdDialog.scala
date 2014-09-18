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

  val model = Utils.deepCopy(initial)

  val fItemId =
    indexedCombo(sm.getProj.data.enums.items, initial.itemId, model.itemId = _)

  val fAddOrRemove = enumIdRadios(AddOrRemove)(
    AddOrRemove.fromBoolean(model.add).id,
    id => model.add = AddOrRemove.toBoolean(id))

  val fQty = new NumberSpinner(1, 1, 99, model.qty = _)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid(lbl("Item:")).add(fItemId)
    row().grid().add(
      new BoxPanel(Orientation.Horizontal) {
        addBtnsAsGrp(contents, fAddOrRemove)
      })
    row().grid(lbl("Quantity:")).add(fQty)

    addButtons(cancelBtn, okBtn)
  }
}