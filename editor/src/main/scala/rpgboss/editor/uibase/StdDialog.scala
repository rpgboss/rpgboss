package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._
import SwingUtils._
import rpgboss.editor.Internationalized._

abstract class StdDialog(owner: Window, titleArg: String)
  extends Dialog(owner) {
  title = titleArg
  modal = true
  defaultButton = okBtn

  private var okPressed = false

  def okFunc()
  def cancelFunc() = {}

  def centerDialog(size : Dimension) = {
    minimumSize = size
    centerOnScreen()
  }

  setLocationRelativeTo(owner)

  lazy val cancelBtn = new Button(Action(getMessage("Cancel")) {
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
    super.close()
    onClose()
  }

  // closeOperation() is only triggered when one presses the "X", not otherwise
  def onClose(): Unit = {
    dispose()
  }

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

class SingleIntegerDialog(
  owner: Window,
  title: String,
  label: String,
  helpMessage: String,
  initial: Int,
  min: Int,
  max: Int,
  okCallback: Int => Unit)
  extends StdDialog(owner, title) {
  val fieldInt = new NumberSpinner(initial, min, max)

  centerDialog(new Dimension(300,100))

  def okFunc() = {
    okCallback(fieldInt.getValue)
    close()
  }

  contents = new DesignGridPanel {
    if (!helpMessage.isEmpty()) {
      row().grid().add(leftLabel(helpMessage))
    }
    row().grid().add(leftLabel(label))
    row().grid().add(fieldInt)
    addButtons(okBtn, cancelBtn)
  }
}
