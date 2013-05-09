package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._

abstract class StdDialog(owner: Window, titleArg: String)
  extends Dialog(owner) {
  title = titleArg
  modal = true
  defaultButton = okBtn
  setLocationRelativeTo(owner)

  private var okPressed = false

  def okFunc()
  def cancelFunc() = {}

  lazy val cancelBtn = new Button(Action("Cancel") {
    cancelFunc()
    close()
  })

  lazy val okBtn = new Button(new Action("OK") {
    mnemonic = Key.O.id
    def apply() = {
      okPressed = true
      okFunc()
    }
  })
  
  override def close() = {
    onClose()
    super.close()
  }

  // closeOperation() is only triggered when one presses the "X", not otherwise
  def onClose(): Unit = {}
  
  /**
   * Treat closing the dialog without pressing OK as a Cancel.
   */
  val me = this
  reactions += {
    case WindowClosing(`me`) => {
      onClose()
      if (!okPressed) cancelFunc()
    }
  }
}
