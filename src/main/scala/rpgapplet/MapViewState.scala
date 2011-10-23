package rpgboss.rpgapplet

import rpgboss.lib.Utils._
import rpgboss.cache._
import rpgboss.model._
import rpgboss.message._

import rpgboss.rpgapplet.tileset._
import rpgboss.rpgapplet.lib._

import scala.math._

trait ListedEnum[T] {
  def valueList : List[T]
  def defaultSelection : T
  var selected = defaultSelection
}

object MapLayers extends Enumeration with ListedEnum[Enumeration#Value] {
  val Bot, Mid, Top, Evt = Value
  val valueList = List(Bot, Mid, Top, Evt)
  def defaultSelection = Bot
}

// The view state for a single map. Needs to be recreated for a different map
class MapViewState(sm: StateMaster, val mapId: Int, val tilecache: TileCache) 
{
  var undoStack: List[RpgMapData] = Nil
  
  def mapMeta = sm.getMapMeta(mapId)
  def mapData = sm.getMapData(mapId)
  
  // map data in editing, for example while mouse is down
  var nextMapData = mapData
  
  def commitNextData() = {
    undoStack = (mapData :: undoStack).take(10)
    sm.setMapData(mapId, nextMapData)
  }
}

