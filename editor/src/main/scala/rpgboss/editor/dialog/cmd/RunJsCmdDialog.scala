package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._

class RunJsCmdDialog(
  owner: Window,
  initial: RunJs,
  successF: (RunJs) => Any)
  extends StdDialog(owner, "Run Javascript") {

  centerDialog(new Dimension(400, 300))

  val textEdit = new TextArea(initial.scriptBody)

  val textEditScroll = new ScrollPane {
    contents = textEdit
    preferredSize = new Dimension(300, 150)
  }

  def okFunc() = {
    successF(RunJs(textEdit.text))
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Script:"))
    row().grid().add(textEditScroll)

    addButtons(cancelBtn, okBtn)
  }

}