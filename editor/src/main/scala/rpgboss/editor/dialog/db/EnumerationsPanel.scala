package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.misc.SwingUtils._
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
        dbDiag.model.enums.elements = array
      }
    }

  val fEquipSubtypes =
    new StringArrayEditingPanel(
      owner,
      "Equipment subtypes",
      dbDiag.model.enums.equipSubtypes) {
      override def onListDataUpdate() = {
        logger.info("Skill types updated")
        dbDiag.model.enums.equipSubtypes = array
      }
    }

  row.grid().add(fElements).add(fEquipSubtypes)
}