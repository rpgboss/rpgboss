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

class EventDialog(
  owner: Window,
  sm: StateMaster,
  val mapName: String,
  initialEvent: RpgEvent,
  onOk: RpgEvent => Any,
  onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + initialEvent.name) {

  var event = initialEvent

  override def cancelFunc() = onCancel(event)

  class EventStatePane(val idx: Int) extends BoxPanel(Orientation.Horizontal) {
    private def curEvtState = event.states(idx)

    val triggerBox = new ComboBox(EventTrigger.values.toSeq) {
      selection.item = EventTrigger(curEvtState.trigger)
    }
    val heightBox = new ComboBox(EventHeight.values.toSeq) {
      selection.item = EventHeight(curEvtState.height)
    }
    val spriteBox = new SpriteField(
      owner,
      sm,
      curEvtState.sprite,
      (spriteSpec: Option[SpriteSpec]) => {
        // If the sprite's "existence" has changed...
        if (curEvtState.sprite.isDefined != spriteSpec.isDefined) {
          heightBox.selection.item = if (spriteSpec.isDefined)
            EventHeight.SAME else EventHeight.UNDER
        }
      })

    contents += new DesignGridPanel {
      row().grid().add(leftLabel("Trigger:"))
      row().grid().add(triggerBox)
      row().grid().add(leftLabel("Height:"))
      row().grid().add(heightBox)
      row().grid().add(leftLabel("Sprite:"))
      row().grid().add(spriteBox)
    }

    val commandBox = new CommandBox(
      EventDialog.this,
      owner,
      sm,
      mapName,
      curEvtState.cmds)

    contents += new DesignGridPanel {
      row.grid.add(leftLabel("Commands:"))
      row.grid.add(new ScrollPane {
        preferredSize = new Dimension(400, 400)
        contents = commandBox
      })
    }

    def formToModel() = {
      val origState = event.states(idx)
      val newState = origState.copy(
        sprite = spriteBox.getSpriteSpec,
        trigger = triggerBox.selection.item.id,
        height = heightBox.selection.item.id,
        cmds = commandBox.listData)
      event = event.copy(states = event.states.updated(idx, newState))
    }
  }

  def okFunc() = {
    event = event.copy(name = nameField.text)

    tabPane.savePanesToModel()

    onOk(event)
    close()
  }

  val nameField = new TextField {
    columns = 12
    text = event.name
  }

  val tabPane = new TabbedPane() {
    def loadPanesFromModel() = {
      pages.clear()
      for (i <- 0 until event.states.length) {
        val state = event.states(i)
        pages += new Page("State %d".format(i), new EventStatePane(i))
      }
    }

    def savePanesToModel() = {
      pages.foreach { page =>
        val pane = page.content.asInstanceOf[EventStatePane]
        pane.formToModel()
      }
    }

    def curPane = selection.page.content.asInstanceOf[EventStatePane]

    loadPanesFromModel()
  }

  def newState(copyCurrent: Boolean) = {
    // Save the current pane statuses
    tabPane.savePanesToModel()

    // Add to list of states
    val newPaneIdx = tabPane.curPane.idx + 1
    val newState =
      if (copyCurrent)
        event.states(tabPane.curPane.idx).copy()
      else
        RpgEventState()
    val newStates =
      event.states.take(newPaneIdx) ++ Array(newState) ++
        event.states.takeRight(event.states.size - newPaneIdx)

    event = event.copy(states = newStates)

    tabPane.loadPanesFromModel()
    tabPane.selection.index = newPaneIdx
  }

  def deleteState() = {
    if (event.states.size == 1) {
      Dialog.showMessage(tabPane, "Cannot delete the last state", "Error",
        Dialog.Message.Error)
    } else {
      // Save the current pane statuses
      tabPane.savePanesToModel()

      val deletedIdx = tabPane.curPane.idx

      val newStates =
        event.states.take(deletedIdx) ++
          event.states.takeRight(event.states.size - deletedIdx - 1)

      event = event.copy(states = newStates)

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
            newState(false)
          }))
        .add(
          new Button(Action("Copy state") {
            newState(true)
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