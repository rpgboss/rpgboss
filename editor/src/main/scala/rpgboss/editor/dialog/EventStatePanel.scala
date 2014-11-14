package rpgboss.editor.dialog

import scala.swing._
import rpgboss.model.event.RpgEventState
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model.event.EventHeight
import rpgboss.editor.resourceselector.SpriteField
import rpgboss.editor.StateMaster
import rpgboss.model.SpriteSpec
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.model.event.EventTrigger
import javax.swing.BorderFactory
import rpgboss.model.MapLoc

/**
 * @param   model   Mutated in-place.
 */
class EventStatePane(
  owner: Window,
  sm: StateMaster,
  model: RpgEventState,
  val idx: Int,
  eventLoc: MapLoc,
  runOnceChangeCallback: () => Unit)
  extends BoxPanel(Orientation.Horizontal) {

  val fSameAppearanceAsPrevState: CheckBox =
    boolField("Same Appearance As Previous State",
        model.sameAppearanceAsPrevState,
        model.sameAppearanceAsPrevState = _,
        Some(updateAppearanceFieldsState))
  val heightBox =
    enumIdCombo(EventHeight)(model.height, model.height = _)

  val spriteBox = new SpriteField(
    owner,
    sm,
    model.sprite,
    (spriteSpec: Option[SpriteSpec]) => {
      // If the sprite's "existence" has changed...
      if (model.sprite.isDefined != spriteSpec.isDefined) {
        heightBox.selection.index =
          if (spriteSpec.isDefined)
            EventHeight.SAME.id
          else
            EventHeight.UNDER.id
      }

      model.sprite = spriteSpec
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
    enumIdCombo(EventTrigger)(model.trigger, model.trigger = _)

  val fRunOnce =
    boolField("Run Once, then increment state",
        model.runOnceThenIncrementState,
        model.runOnceThenIncrementState = _,
        Some(runOnceChangeCallback))

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
    owner,
    sm,
    eventLoc,
    model.cmds,
    newCmds => {
      model.cmds = newCmds
      messageField.updateMessages()
    },
    inner = false)

  val messageField = new TextField() {
    enabled = true
    editable = false

    def updateMessages() = {
      val freeVars = model.getFreeVariables()
      if (!freeVars.isEmpty) {
        text = "Free variables (normal for event classes): %s.".format(
            freeVars.map(_.localVariable).mkString(", "))
      } else {
        text = "No errors."
      }
    }
    updateMessages()
  }

  contents += new DesignGridPanel {
    row.grid.add(leftLabel("Commands:"))
    row.grid.add(new ScrollPane {
      preferredSize = new Dimension(400, 400)
      contents = commandBox
    })

    row.grid.add(messageField)
  }
}