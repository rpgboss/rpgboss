package rpgboss.editor

import rpgboss.lib.Utils._
import rpgboss.model._
import rpgboss.model.resource._

import rpgboss.editor.tileset._
import rpgboss.editor.lib._
import rpgboss.editor.cache._

import scala.math._

object MapLayers extends RpgEnum {
  val Bot, Mid, Top, Evt = Value

  def drawOrderZip[T](list: List[T]) =
    List(Bot, Mid, Top) zip list

  def mapOfArrays(mapData: RpgMapData): Map[Enumeration#Value, Array[Array[Byte]]] =
    Map(drawOrderZip(mapData.drawOrder): _*)

  def default = Bot
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
class MapViewState(val sm: StateMaster, val mapName: String) {
  var prevStates: List[RpgMapData] = List()

  def map = sm.getMap(mapName)
  def mapMeta = map.metadata

  val tileCache = new MapTileCache(sm.assetCache, map)

  // Map data in editing, for example while mouse is down.
  // Think of this as the working copy
  // We can undo to a previous state
  var nextMapData = sm.getMapData(mapName).deepcopy()

  var inTransaction = false

  // Initialize the prevStates list with the initial state
  prevStates = List(nextMapData.deepcopy())

  def undo() = {
    prevStates = prevStates.tail
    nextMapData = prevStates.head.deepcopy()
  }

  def canUndo() = {
    prevStates.size > 1
  }

  def begin() = {
    if (inTransaction) {
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
    if (inTransaction) {
      prevStates = (nextMapData :: prevStates).take(10)
      nextMapData = nextMapData.deepcopy()
      sm.setMapData(mapName, nextMapData)
      inTransaction = false
    } else {
      throw new RuntimeException("Can't commit outside of transaction.")
    }
  }
}

