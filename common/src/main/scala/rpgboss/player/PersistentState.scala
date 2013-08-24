package rpgboss.player
import collection.mutable._

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState {
  val cameraLoc = new MutableMapLoc()
  val pictures = Array.fill[Option[PictureInfo]](32)(None)

  private val globals =
    new HashMap[String, Int]() with SynchronizedMap[String, Int]

  private val globalArrays =
    new HashMap[String, Array[Int]] with SynchronizedMap[String, Array[Int]]

  // mapName->evtName->variableName
  private val eventStates = new HashMap[String, Map[String, Int]]()

  // TODO: save player location

  def setGlobal(key: String, value: Int) = globals.update(key, value)
  def getGlobal(key: String) = globals.getOrElseUpdate(key, 0)

  def getArray(key: String) = 
    globalArrays.getOrElseUpdate(key, new Array[Int](0))
  def setArray(key: String, value: Array[Int]) = globalArrays.update(key, value)
  
  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, evtName: String) = {
    eventStates.get(mapName).map { stateMapForCurMap =>
      stateMapForCurMap.get(evtName) getOrElse 0
    } getOrElse 0
  }

  def setEventState(mapName: String, eventName: String, newState: Int) = {
    eventStates.getOrElseUpdate(
      mapName, new HashMap[String, Int] with SynchronizedMap[String, Int])
      .update(eventName, newState)
  }
}