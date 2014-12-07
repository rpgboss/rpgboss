package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.Internationalized._

class ShowTextCmdDialog(
  owner: Window,
  initial: ShowText,
  successF: (ShowText) => Any)
  extends StdDialog(owner, getMessage("Show_Text")) {

  centerDialog(new Dimension(400, 300))

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

    addButtons(okBtn, cancelBtn)
  }

}