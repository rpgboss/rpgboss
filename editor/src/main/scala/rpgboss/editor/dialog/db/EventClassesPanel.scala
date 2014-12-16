package rpgboss.editor.dialog.db

import scala.swing._

import net.java.dev.designgridlayout._
import rpgboss.editor._
import rpgboss.editor.dialog._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.event.EventClass
import rpgboss.editor.Internationalized._

class EventClassesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.eventClasses)
  with DatabasePanel {
  def panelName = getMessage("Event_Classes")
  def newDefaultInstance() = new EventClass()

  def editPaneForItem(idx: Int, model: EventClass) = {
    new EventPanel(
        owner,
        sm,
        None,
        model.name,
        newName => {
          model.name = newName
          refreshModel()
        },
        model.states,
        model.states = _) with DisposableComponent
  }

  override def onListDataUpdate() = {
    logger.info(getMessage("Event_Classes_Updated"))
    dbDiag.model.enums.eventClasses = dataAsArray
  }
}