package rpgboss.editor.dialog.db
import rpgboss.model.ProjectData

trait DatabasePanel {
  def panelName: String
  def updated(data: ProjectData): ProjectData
}