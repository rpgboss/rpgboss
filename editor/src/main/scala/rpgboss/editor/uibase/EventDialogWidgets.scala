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
import javax.swing.BorderFactory
import rpgboss.editor.misc.MapSelector

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
  model: EntitySpec,
  allowPlayer: Boolean,
  allowEventOnOtherMap: Boolean)
  extends DesignGridPanel {

  border = BorderFactory.createTitledBorder("Which Entity")

  def updateFieldState() = {
    val which = WhichEntity(model.whichEntityId)
    fieldEventId.enabled =
      which == WhichEntity.EVENT_ON_MAP ||
      which == WhichEntity.EVENT_ON_OTHER_MAP
  }

  val disabledSet = collection.mutable.Set[WhichEntity.Value]()
  if (!allowPlayer) disabledSet += WhichEntity.PLAYER
  if (!allowEventOnOtherMap) disabledSet += WhichEntity.EVENT_ON_OTHER_MAP
  if (mapData.events.isEmpty) disabledSet += WhichEntity.EVENT_ON_MAP

  val btns = enumIdRadios(WhichEntity)(
    model.whichEntityId,
    v => {
      model.whichEntityId = v
      updateFieldState()
    },
    disabledSet = disabledSet.toSet)

  val mapSelector = new MapSelector(sm)

  val (fieldEventId, _) =
    EventArrayComboBox.fromMap(mapData, model.eventId, model.eventId = _)

  row().grid().add(new BoxPanel(Orientation.Vertical) {
    addBtnsAsGrp(contents, btns)
  })

  row().grid(new Label("Event: ")).add(fieldEventId)

  if (allowEventOnOtherMap)
    row().grid(new Label("Map: ")).add(mapSelector)

  updateFieldState();
}