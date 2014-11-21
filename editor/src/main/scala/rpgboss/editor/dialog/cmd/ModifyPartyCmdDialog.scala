package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.StateMaster
import rpgboss.model.RpgEnum
import rpgboss.model.AddOrRemove

class ModifyPartyCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: ModifyParty,
  successF: (ModifyParty) => Any)
  extends StdDialog(owner, "Show text") {

  centerDialog(new Dimension(200, 100))

  var modelAddOrRemove = initial.add

  val fAddOrRemove = enumIdRadios(AddOrRemove)(
    AddOrRemove.fromBoolean(modelAddOrRemove).id,
    id => modelAddOrRemove = AddOrRemove.toBoolean(id))
  val fCharacterId = indexedCombo(
    sm.getProjData.enums.characters, initial.characterId, _ => Unit)


  def okFunc() = {
    successF(ModifyParty(modelAddOrRemove, fCharacterId.selection.index))
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(
      new BoxPanel(Orientation.Horizontal) {
        addBtnsAsGrp(contents, fAddOrRemove)
      })
    row().grid(lbl("Character: ")).add(fCharacterId)

    addButtons(okBtn, cancelBtn)
  }

}