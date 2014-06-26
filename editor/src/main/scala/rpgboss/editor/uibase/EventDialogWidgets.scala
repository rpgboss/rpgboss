package rpgboss.editor.uibase

import rpgboss.model.RpgMapData
import rpgboss.model.event.RpgEvent
import scala.swing.event.SelectionChanged
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model.EntitySpec
import scala.swing._
import rpgboss.editor.StateMaster
import rpgboss.editor.resourceselector.MapField
import rpgboss.model.WhichEntity

object EventArrayComboBox {
  // Returns a ComboBox, plus a boolean indicating whether or not 'initial' was
  // found and selected in the ComboBox
  def fromMap(mapData: RpgMapData, initialId: Int, onUpdate: Int => Unit) :
    (ComboBox[RpgEvent], Boolean) = {
    val events : Vector[RpgEvent] = mapData.events.values.toVector.sortBy(_.id)
    val indexOfInitialInArray = events.indexWhere(_.id == initialId)
    val initialIdFound = indexOfInitialInArray != -1

    // If no events exist, both the selection and custom renderer won't work,
    // so we provide a dummy disabled comboBox.
    val comboBox = if (events.isEmpty) {
      new ComboBox(Array[RpgEvent]()) {
        enabled = false
      }
    } else {
      // Update the initialId if it wasn't found.
      if (!initialIdFound)
        onUpdate(events.head.id)

      new ComboBox[RpgEvent](events) {
        selection.index = if (initialIdFound) indexOfInitialInArray else 0

        renderer = ListView.Renderer(event => {
          "%d: %s".format(event.id, event.name)
        })

        listenTo(this.selection)
        reactions += {
          case SelectionChanged(_) => onUpdate(selection.item.id)
        }
      }
    }

    (comboBox, initialIdFound)
  }
}

/**
 * @param   model   Is mutated in place.
 */
class EntitySelectPanel(
  owner: Window,
  sm: StateMaster,
  mapData: RpgMapData,
  model: EntitySpec)
  extends DesignGridPanel {

  def updateFieldState() = {
    fieldEventId.enabled =
      WhichEntity(model.whichEntityId) == WhichEntity.OTHER_EVENT
  }

  val btns = enumIdRadios(WhichEntity)(
    model.whichEntityId,
    v => {
      model.whichEntityId = v
      updateFieldState()
    },
    disabledSet =
      if (mapData.events.isEmpty) Set(WhichEntity.OTHER_EVENT) else Set.empty)

  val (fieldEventId, found) =
    EventArrayComboBox.fromMap(mapData, model.eventId, model.eventId = _)

  row().grid().add(new BoxPanel(Orientation.Vertical) {
    addBtnsAsGrp(contents, btns)
  })
  row().grid().add(fieldEventId)

  updateFieldState();
}