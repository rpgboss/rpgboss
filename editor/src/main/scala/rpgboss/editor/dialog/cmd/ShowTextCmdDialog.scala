package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.misc.SwingUtils._
import rpgboss.editor.uibase._

class ShowTextCmdDialog(
  owner: Window,
  initial: ShowText,
  successF: (ShowText) => Any)
  extends StdDialog(owner, "Show text") {

  val textEdit = new TextArea(initial.lines.mkString("\n"))

  val textEditScroll = new ScrollPane {
    contents = textEdit
    preferredSize = new Dimension(300, 150)
  }

  def okFunc() = {
    successF(ShowText(textEdit.text.split("\n")))
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Text:"))
    row().grid().add(textEditScroll)

    addButtons(cancelBtn, okBtn)
  }

}