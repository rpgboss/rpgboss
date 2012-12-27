package rpgboss.editor.dialog.db
import rpgboss.model.ProjectData
import rpgboss.editor.dialog.DatabaseDialog

trait DatabasePanel {
  def panelName: String
  def dbDiag: DatabaseDialog
}