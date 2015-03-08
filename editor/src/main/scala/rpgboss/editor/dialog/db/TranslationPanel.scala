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

class TranslationPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel {
  def panelName = getMessage("Translations")
  layout.labelAlignment(LabelAlignment.RIGHT)

  val fElements =
    new StringArrayEditingPanel(
      owner,
      getMessage("Translations"),
      dbDiag.model.enums.translationTypes,
      dbDiag.model.enums.translationTypes = _)

  row.grid().add(fElements)
}