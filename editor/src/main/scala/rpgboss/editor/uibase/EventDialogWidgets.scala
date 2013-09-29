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
  def fromMap(mapData: RpgMapData, initial: Int, onUpdate: Int => Unit) :
    (ComboBox[(RpgEvent, Int)], Boolean) = {
    val nonDeletedEventsWithIndices : Array[(RpgEvent, Int)] =
      mapData.events.zipWithIndex.filter(!_._1.deleted)
    
    val indexInFilteredList = 
      nonDeletedEventsWithIndices.indexWhere(_._2 == initial)
    
    val found = indexInFilteredList != -1
      
    val comboBox = new ComboBox(nonDeletedEventsWithIndices) {
      selection.index = if (found) indexInFilteredList else 0

      renderer = ListView.Renderer(eventTuple => {
        "%d: %s".format(eventTuple._2, eventTuple._1.name)
      })

      reactions += {
        case SelectionChanged(_) => onUpdate(selection.item._2)
      }
    }
    
    (comboBox, found)
  }
}

class EntitySelectPanel(
  owner: Window,
  sm: StateMaster,
  mapData: RpgMapData,
  initial: EntitySpec, 
  updateF: EntitySpec => Unit)
  extends DesignGridPanel {
  
  // model is mutable
  val model = initial
  
  def updateFieldState() = {
    fieldEventIdx.enabled =
      WhichEntity(model.whichEntityId) == WhichEntity.OTHER_EVENT
  }
  
  val btns = enumRadios(WhichEntity)(
    WhichEntity(model.whichEntityId), 
    v => {
      model.whichEntityId = v.id
      updateFieldState()
    })
  
  val (fieldEventIdx, found) = 
    EventArrayComboBox.fromMap(mapData, model.eventIdx, model.eventIdx = _)
  
  row().grid(lbl("Which Event:")).add(new BoxPanel(Orientation.Vertical) {
    addBtnsAsGrp(contents, btns)
  })
  row().grid(lbl("Specified Event:")).add(fieldEventIdx)
}