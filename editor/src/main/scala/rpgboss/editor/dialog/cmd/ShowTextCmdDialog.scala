package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.Internationalized._
import rpgboss.editor.StateMaster

class ShowTextCmdDialog(
  owner: Window,
  sm: StateMaster,
  initial: ShowText,
  successF: (ShowText) => Any)
  extends EventCmdDialog(
      owner, sm, getMessage("Show_Text"), initial, successF) {

  override def extraFields = Seq(
    TitledComponent("Text", textAreaField(model.lines, model.lines = _)))
}