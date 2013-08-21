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

  // mapName->evtName->variableName
  private val eventStates = new HashMap[String, Map[String, Int]]()

  // TODO: save player location
  
  def setGlobal(key: String, value: Int) = globals.update(key, value)
  def getGlobal(key: String) = globals.getOrElse(key, 0)
  
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