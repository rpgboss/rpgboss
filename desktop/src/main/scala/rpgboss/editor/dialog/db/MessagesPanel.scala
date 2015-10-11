package rpgboss.editor.dialog.db

import scala.swing.Window

import rpgboss.editor.Internationalized.getMessage
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.StringMapEditingPanel

class MessagesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel {
  def panelName = getMessage("Messages")

  val fMessages = new StringMapEditingPanel(
    owner,
    getMessage("Messages"),
    dbDiag.model.messages,
    dbDiag.model.messages = _)

  row.grid().add(fMessages)
}