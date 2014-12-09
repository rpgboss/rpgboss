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

class EnumerationsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel {
  def panelName = getMessage("Enumerations")
  layout.labelAlignment(LabelAlignment.RIGHT)

  val fElements =
    new StringArrayEditingPanel(
      owner,
      "Elements",
      dbDiag.model.enums.elements,
      dbDiag.model.enums.elements = _)

  val fEquipTypes =
    new StringArrayEditingPanel(
      owner,
      "Equipment Types",
      dbDiag.model.enums.equipTypes,
      dbDiag.model.enums.equipTypes = _)

  row.grid().add(fElements).add(fEquipTypes)
}