package rpgboss.player
import collection.mutable._

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState {
  val cameraLoc = new MutableMapLoc()
  val pictures = Array.fill[Option[PictureInfo]](32)(None)

  private val globalInts =
    new HashMap[String, Int]() with SynchronizedMap[String, Int]

  private val intArrays =
    new HashMap[String, Array[Int]] with SynchronizedMap[String, Array[Int]]

  private val stringArrays =
    new HashMap[String, Array[String]] 
      with SynchronizedMap[String, Array[String]]
  
  // mapName->eventId->state
  private val eventStates = new HashMap[String, Map[Int, Int]]()

  // TODO: save player location

  def setInt(key: String, value: Int) = globalInts.update(key, value)
  def getInt(key: String) = globalInts.getOrElseUpdate(key, 0)

  def getIntArray(key: String) = 
    intArrays.getOrElseUpdate(key, new Array[Int](0))
  def setIntArray(key: String, value: Array[Int]) = 
    intArrays.update(key, value)
  
  def getStringArray(key: String) = 
    stringArrays.getOrElseUpdate(key, new Array[String](0))
  def setStringArray(key: String, value: Array[String]) = 
    stringArrays.update(key, value)
    
  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, eventId: Int) = {
    eventStates.get(mapName).map { stateMapForCurMap =>
      stateMapForCurMap.get(eventId) getOrElse 0
    } getOrElse 0
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = {
    eventStates.getOrElseUpdate(
      mapName, new HashMap[Int, Int] with SynchronizedMap[Int, Int])
      .update(eventId, newState)
  }
}