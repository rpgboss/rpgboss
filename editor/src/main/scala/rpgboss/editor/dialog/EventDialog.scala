package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.uibase._
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page
import rpgboss.model._
import rpgboss.editor.StateMaster
import java.awt.Dimension
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.resourceselector.SpriteField
import javax.swing.BorderFactory
import rpgboss.lib.Utils

class EventDialog(
  owner: Window,
  sm: StateMaster,
  val mapName: String,
  initialEvent: RpgEvent,
  onOk: RpgEvent => Any,
  onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + initialEvent.name) {

  centerDialog(new Dimension(600, 600))

  // TODO: Need to make a deep copy here for Cancel to work correctly.
  // However, Utils.deepCopy doesn't work correctly for polymorphic lists.
  val event = initialEvent

  override def cancelFunc() = onCancel(event)

  def okFunc() = {
    onOk(event)
    close()
  }

  val nameField = textField(event.name, event.name = _)

  val tabPane = new TabbedPane() {
    def loadPanesFromModel(): Unit = {
      pages.clear()
      for (i <- 0 until event.states.length) {
        val state = event.states(i)
        val pane: EventStatePane = new EventStatePane(
            owner,
            sm,
            state,
            i,
            MapLoc(mapName, event.x, event.y),
            () => {
              if (i == event.states.length - 1) {
                newState(false, false)
              }
            })
        pages += new Page("State %d".format(i), pane)
      }
    }

    def curPane = selection.page.content.asInstanceOf[EventStatePane]
  }

  tabPane.loadPanesFromModel()

  def newState(copyCurrent: Boolean, switchPane: Boolean): Unit = {
    // Add to list of states
    val newPaneIdx = tabPane.curPane.idx + 1
    val newState =
      if (copyCurrent)
        event.states(tabPane.curPane.idx).copy()
      else
        RpgEventState()
    event.states =
      event.states.take(newPaneIdx) ++ Array(newState) ++
        event.states.takeRight(event.states.size - newPaneIdx)

    tabPane.loadPanesFromModel()

    if (switchPane) {
      tabPane.selection.index = newPaneIdx
    }
  }

  def deleteState() = {
    if (event.states.size == 1) {
      Dialog.showMessage(tabPane, "Cannot delete the last state", "Error",
        Dialog.Message.Error)
    } else {
      val deletedIdx = tabPane.curPane.idx

      event.states =
        event.states.take(deletedIdx) ++
          event.states.takeRight(event.states.size - deletedIdx - 1)

      tabPane.loadPanesFromModel()
      tabPane.selection.index = math.min(deletedIdx, event.states.size - 1)
    }
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid()
        .add(leftLabel("Name:")).add(nameField)
        .add(
          new Button(Action("New state") {
            newState(false, true)
          }))
        .add(
          new Button(Action("Copy state") {
            newState(true, true)
          }))
        .add(
          new Button(Action("Delete state") {
            deleteState()
          }))

      row.grid().add(tabPane)

      addButtons(cancelBtn, okBtn)
    }
  }
}