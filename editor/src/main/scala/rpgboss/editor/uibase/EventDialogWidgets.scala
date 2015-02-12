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
import rpgboss.model.resource.RpgMap
import rpgboss.editor.Internationalized._

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
 * @param   model       Is mutated in place.
 * @param   mapData     Is the map data for the current map.
 */
class EntitySelectPanel(
  owner: Window,
  sm: StateMaster,
  currentMapName: Option[String],
  model: EntitySpec,
  allowPlayer: Boolean,
  allowEventOnOtherMap: Boolean)
  extends DesignGridPanel {

  border = BorderFactory.createTitledBorder(getMessage("Which_Entity"))

  override def enabled_=(b: Boolean) = {
    super.enabled = b

    if (!b) {
      mapSelector.enabled = false
      if (fieldEventId != null)
        fieldEventId.enabled = false
      btns.foreach(_.enabled = false)
    } else {
      updateFieldState(model.whichEntityId, model.whichEntityId)
      btns.foreach(btn => btn.enabled = !disabledSet.contains(btn.value))
    }
  }

  var selectedOtherMap =
    if (model.whichEntityId == WhichEntity.EVENT_ON_OTHER_MAP.id)
      model.mapName
    else
      currentMapName.getOrElse("")
  val selectedEventIdPerMap = collection.mutable.HashMap[String, Int]()

  val currentMapData = currentMapName.map(sm.getMapData)

  def updateFieldState(oldWhichId: Int, newWhichId: Int) = {
    val oldWhich = WhichEntity(oldWhichId)
    val newWhich = WhichEntity(newWhichId)

    // Persist old stuff
    if (oldWhich == WhichEntity.EVENT_ON_OTHER_MAP)
      selectedOtherMap = model.mapName

    val oldMapName = model.mapName

    // Restore new mode stuff
    if (newWhich == WhichEntity.EVENT_ON_MAP)
      model.mapName = currentMapName.getOrElse("")
    else if (newWhich == WhichEntity.EVENT_ON_OTHER_MAP)
      model.mapName = selectedOtherMap

    replaceEventIdField(oldMapName, model.mapName)

    mapSelector.enabled = newWhich == WhichEntity.EVENT_ON_OTHER_MAP
    fieldEventId.enabled =
      newWhich == WhichEntity.EVENT_ON_MAP ||
      newWhich == WhichEntity.EVENT_ON_OTHER_MAP
  }

  var fieldEventId: ComboBox[RpgEvent] = null

  def replaceEventIdField(oldMapName: String, newMapName: String) = {
    selectedEventIdPerMap(oldMapName) = model.eventId
    if (newMapName != "") {
      val newMapData = sm.getMapData(newMapName)

      val initialId = selectedEventIdPerMap.getOrElse(newMapName, -1)

      val (newField, found) =
          EventArrayComboBox.fromMap(newMapData, initialId, newEventId => {
            model.eventId = newEventId
          })

      if (!newMapData.events.isEmpty)
        model.eventId = newField.selection.item.id
      else
        model.eventId = -1

      fieldEventId = newField
    } else {
      fieldEventId = new ComboBox[RpgEvent](Nil) {
        enabled = false
      }
      model.eventId = -1
    }

    fieldEventIdContainer.contents.clear()
    fieldEventIdContainer.contents += fieldEventId
    fieldEventIdContainer.revalidate()
  }

  val fieldEventIdContainer = new BoxPanel(Orientation.Vertical)

  val disabledSet = collection.mutable.Set[WhichEntity.Value]()
  if (!allowPlayer) disabledSet += WhichEntity.PLAYER
  if (!allowEventOnOtherMap) disabledSet += WhichEntity.EVENT_ON_OTHER_MAP
  if (currentMapData.isEmpty || currentMapData.get.events.isEmpty)
    disabledSet += WhichEntity.EVENT_ON_MAP

  val btns = enumIdRadios(WhichEntity)(
    model.whichEntityId,
    v => {
      val old = model.whichEntityId
      model.whichEntityId = v
      updateFieldState(old, v)
    },
    disabledSet = disabledSet.toSet)

  val mapSelector = new MapSelector(sm) {
    override def onSelectMap(mapOpt: Option[RpgMap]) = {
      for (map <- mapOpt) {
        replaceEventIdField(model.mapName, map.name)
        model.mapName = map.name
      }
    }
  }

  // Initialize the combo box data
  if (model.whichEntityId == WhichEntity.EVENT_ON_OTHER_MAP.id)
    mapSelector.getNode(model.mapName).map(mapSelector.selectNode)
  else
    currentMapName.flatMap(mapSelector.getNode).map(mapSelector.selectNode)

  row().grid().add(new BoxPanel(Orientation.Vertical) {
    addBtnsAsGrp(contents, btns)
  })

  row().grid(new Label(getMessageColon("Event"))).add(fieldEventIdContainer)

  if (allowEventOnOtherMap)
    row().grid(new Label(getMessageColon("Map"))).add(mapSelector)

  updateFieldState(model.whichEntityId, model.whichEntityId)
}