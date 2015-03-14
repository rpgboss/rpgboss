package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.Internationalized._

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