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

  val event = Utils.deepCopy(initialEvent)

  override def cancelFunc() = onCancel(event)

  class EventStatePane(val idx: Int) extends BoxPanel(Orientation.Horizontal) {
    private def curEvtState = event.states(idx)

    val fSameAppearanceAsPrevState: CheckBox =
      boolField("Same Appearance As Previous State",
          curEvtState.sameAppearanceAsPrevState,
          curEvtState.sameAppearanceAsPrevState = _,
          Some(updateAppearanceFieldsState))
    val heightBox =
      enumIdCombo(EventHeight)(curEvtState.height, curEvtState.height = _)

    val spriteBox = new SpriteField(
      owner,
      sm,
      curEvtState.sprite,
      (spriteSpec: Option[SpriteSpec]) => {
        // If the sprite's "existence" has changed...
        if (curEvtState.sprite.isDefined != spriteSpec.isDefined) {
          heightBox.selection.index =
            if (spriteSpec.isDefined)
              EventHeight.SAME.id
            else
              EventHeight.UNDER.id
        }

        curEvtState.sprite = spriteSpec
      })

    def updateAppearanceFieldsState() = {
      if (idx == 0) {
        fSameAppearanceAsPrevState.selected = true
        fSameAppearanceAsPrevState.enabled = false
        heightBox.enabled = true
        spriteBox.enabled = true
      } else {
        val sameAppearance = fSameAppearanceAsPrevState.selected
        heightBox.enabled = !sameAppearance
        spriteBox.enabled = !sameAppearance
      }
    }
    updateAppearanceFieldsState()

    val triggerBox =
      enumIdCombo(EventTrigger)(curEvtState.trigger, curEvtState.trigger = _)

    val fRunOnce =
      boolField("Run Once, then increment state",
          curEvtState.runOnceThenIncrementState,
          curEvtState.runOnceThenIncrementState = _,
          Some(() => {
            if (curEvtState == event.states.last) {
              newState(false, false)
            }
          }))

    contents += new BoxPanel(Orientation.Vertical) {
      contents += new DesignGridPanel {
        border = BorderFactory.createTitledBorder("Appearance")

        row().grid().add(fSameAppearanceAsPrevState)
        row().grid().add(leftLabel("Height:"))
        row().grid().add(heightBox)
        row().grid().add(leftLabel("Sprite:"))
        row().grid().add(spriteBox)
      }

      contents += new DesignGridPanel {
        border = BorderFactory.createTitledBorder("Behavior")
        row().grid().add(leftLabel("Trigger:"))
        row().grid().add(triggerBox)
        row().grid().add(fRunOnce)
      }
    }

    val commandBox = new CommandBox(
      EventDialog.this,
      owner,
      sm,
      mapName,
      curEvtState.cmds,
      curEvtState.cmds = _,
      inner = false)

    contents += new DesignGridPanel {
      row.grid.add(leftLabel("Commands:"))
      row.grid.add(new ScrollPane {
        preferredSize = new Dimension(400, 400)
        contents = commandBox
      })
    }
  }

  def okFunc() = {
    onOk(event)
    close()
  }

  val nameField = textField(event.name, event.name = _)

  val tabPane = new TabbedPane() {
    def loadPanesFromModel() = {
      pages.clear()
      for (i <- 0 until event.states.length) {
        val state = event.states(i)
        pages += new Page("State %d".format(i), new EventStatePane(i))
      }
    }

    def curPane = selection.page.content.asInstanceOf[EventStatePane]

    loadPanesFromModel()
  }

  def newState(copyCurrent: Boolean, switchPane: Boolean) = {
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