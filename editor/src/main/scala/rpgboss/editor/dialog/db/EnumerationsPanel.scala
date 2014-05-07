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

class EnumerationsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel {
  def panelName = "Enumerations"
  layout.labelAlignment(LabelAlignment.RIGHT)

  val fElements =
    new StringArrayEditingPanel(
      owner,
      "Elements",
      dbDiag.model.enums.elements) {

      override def onListDataUpdate() = {
        logger.info("Elements updated")
        dbDiag.model.enums.elements = dataAsArray
      }
    }

  val fEquipTypes =
    new StringArrayEditingPanel(
      owner,
      "Equipment Types",
      dbDiag.model.enums.equipTypes) {
      override def onListDataUpdate() = {
        dbDiag.model.enums.equipTypes = dataAsArray
      }
    }

  row.grid().add(fElements).add(fEquipTypes)
}