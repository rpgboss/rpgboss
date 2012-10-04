package rpgboss.editor.dialog

import rpgboss.editor.lib._
import scala.swing._
import scala.swing.event._

abstract class StdDialog(owner: Window, titleArg: String) 
  extends Dialog(owner) with HttpSender
{
  title = titleArg
  modal = true
  defaultButton = okButton
  setLocationRelativeTo(owner)
  
  def okFunc()
  def cancelFunc() = close()
  
  def leftLabel(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }
  
  lazy val cancelButton = new Button(Action("Cancel") { cancelFunc() })
  
  lazy val okButton = new Button(new Action("OK") {
    mnemonic = Key.O.id
    def apply() = okFunc
  })
}
