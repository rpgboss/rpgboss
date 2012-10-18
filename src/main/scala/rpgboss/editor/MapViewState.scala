package rpgboss.editor

import rpgboss.lib.Utils._
import rpgboss.cache._
import rpgboss.model._
import rpgboss.model.resource._

import rpgboss.editor.tileset._
import rpgboss.editor.lib._

import scala.math._

trait ListedEnum[T] {
  def valueList : List[T]
  private var selectedVar : T = null.asInstanceOf[T]
  
  def selected : T = selectedVar
  def selected_=(x: T) = selectedVar = x 
}

object MapLayers extends Enumeration with ListedEnum[Enumeration#Value] {
  val Bot, Mid, Top, Evt = Value
  val valueList = List(Bot, Mid, Top, Evt)
  
  def enumDrawOrder(mapData: RpgMapData) = 
    List(Bot, Mid, Top) zip mapData.drawOrder
  
  def mapOfArrays(mapData: RpgMapData) : Map[Enumeration#Value, Array[Array[Byte]]] = 
    Map(enumDrawOrder(mapData) : _*)
  
  selected = Bot
}

/**
 *  The view state for a single map. Needs to be recreated for a different map.
 *  
 *  This implements simple transaction semantics. This is needed because
 *  editing tools often make temporary changes that will be displayed, but not
 *  necessarily desired, i.e. a rectangle or ellipse tool.
 *  
 *  These transactions will be often aborted when in such circumstances.
 *  
 *  This class also provides a stack of old states for an undo mechanism.
 */
class MapViewState(val sm: StateMaster, val mapName: String, 
                   val tilecache: TileCache) 
{
  var prevStates: List[RpgMapData] = List()
  
  def map = sm.getMap(mapName)
  def mapMeta = map.metadata
  
  // Map data in editing, for example while mouse is down.
  // Need to be able to get the previous map data from the undo stack
  // Think of this as the working copy
  var nextMapData = sm.getMapData(mapName)
  
  var inTransaction = false
  
  // Initialize the prevStates list with the initial state
  prevStates = List(nextMapData)
  
  def begin() = {
    if(inTransaction) {
      throw new RuntimeException(
          "Tried to enter MapViewState transaction while already in one")    
    } else {
      inTransaction = true
    }
  }
  
  def abort() = {
    nextMapData = prevStates.head
    inTransaction = false
  }
  
  def commit() = {
    if(inTransaction) {
      prevStates = (nextMapData :: prevStates).take(10)
      sm.setMapData(mapName, nextMapData)
      inTransaction = false
    } else {
      throw new RuntimeException("Can't commit outside of transaction.")
    }
  }
}

