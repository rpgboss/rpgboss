package rpgboss.editor.dialog.db
import rpgboss.model.ProjectData
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.uibase.RightPaneArrayEditingPanel
import scala.swing._
import rpgboss.model.HasName
import rpgboss.editor.uibase.DisposableComponent

trait DatabasePanel extends DisposableComponent {
  def panelName: String
  def dbDiag: DatabaseDialog

  def activate(): Unit = {}
}

abstract class RightPaneArrayDatabasePanel[T <: HasName](
  owner: Window,
  label: String,
  initialAry: Array[T])(implicit m: Manifest[T])
  extends RightPaneArrayEditingPanel[T](owner, label, initialAry)(m)
  with DatabasePanel {

  def label(a: T) = a.name

  override def activate(): Unit = {
    if (!listView.selection.indices.isEmpty)
      listView.selectIndices(listView.selection.indices.head)
  }

  override def dispose() = {
    super[RightPaneArrayEditingPanel].dispose()
  }
}