package rpgboss.editor.dialog

import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model.MapLoc
import scala.swing.Window
import scala.swing.BoxPanel
import scala.swing.TabbedPane
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.model.event.RpgEventState
import rpgboss.lib.Utils
import scala.swing.Button
import scala.swing.Dialog
import scala.swing.TabbedPane.Page
import scala.swing.Action
import rpgboss.model.RpgMapData

class EventPanel(
  owner: Window,
  sm: StateMaster,
  eventLoc: MapLoc,
  initialName: String,
  updateNameF: String => Unit,
  initialStates: Array[RpgEventState],
  updateStatesF: Array[RpgEventState] => Unit) extends DesignGridPanel {

  private var statesModel = Utils.deepCopy(initialStates)
  def updateStates(newStatesModel: Array[RpgEventState]) = {
    statesModel = newStatesModel
    updateStatesF(statesModel)
  }

  val nameField = textField(initialName, updateNameF)

  val tabPane = new TabbedPane() {
    def loadPanesFromModel(): Unit = {
      pages.clear()
      for (i <- 0 until statesModel.length) {
        val state = statesModel(i)
        val pane: EventStatePane = new EventStatePane(
            owner,
            sm,
            state,
            i,
            eventLoc,
            () => {
              if (i == statesModel.length - 1) {
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
        statesModel(tabPane.curPane.idx).copy()
      else
        RpgEventState()
    updateStates(
      statesModel.take(newPaneIdx) ++ Array(newState) ++
        statesModel.takeRight(statesModel.size - newPaneIdx))

    tabPane.loadPanesFromModel()

    if (switchPane) {
      tabPane.selection.index = newPaneIdx
    }
  }

  def deleteState() = {
    if (statesModel.size == 1) {
      Dialog.showMessage(tabPane, "Cannot delete the last state", "Error",
        Dialog.Message.Error)
    } else {
      val deletedIdx = tabPane.curPane.idx

      updateStates(
        statesModel.take(deletedIdx) ++
          statesModel.takeRight(statesModel.size - deletedIdx - 1))

      tabPane.loadPanesFromModel()
      tabPane.selection.index = math.min(deletedIdx, statesModel.size - 1)
    }
  }

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
}