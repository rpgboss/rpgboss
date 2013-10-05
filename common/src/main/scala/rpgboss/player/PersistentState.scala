package rpgboss.player
import collection.mutable.{HashMap => MutableHashMap}
import collection.mutable.{SynchronizedMap => MutableSynchronizedMap}

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState {
  val cameraLoc = new MutableMapLoc()
  val pictures = Array.fill[Option[PictureInfo]](32)(None)

  private val globalInts =
    new MutableHashMap[String, Int]() 
    with MutableSynchronizedMap[String, Int]

  private val intArrays =
    new MutableHashMap[String, Array[Int]] 
    with MutableSynchronizedMap[String, Array[Int]]

  private val stringArrays =
    new MutableHashMap[String, Array[String]] 
    with MutableSynchronizedMap[String, Array[String]]
  
  // mapName->eventId->state
  private val eventStates = 
    new MutableHashMap[String, MutableHashMap[Int, Int]]()

  // TODO: save player location

  def setInt(key: String, value: Int) = globalInts.update(key, value)
  def getInt(key: String) = globalInts.getOrElseUpdate(key, 0)

  def getIntArray(key: String) = 
    intArrays.getOrElseUpdate(key, new Array[Int](0))
  def setIntArray(key: String, value: Seq[Int]) = 
    intArrays.update(key, value.toArray)
  
  def getStringArray(key: String) = 
    stringArrays.getOrElseUpdate(key, new Array[String](0))
  def setStringArray(key: String, value: Seq[String]) = 
    stringArrays.update(key, value.toArray)
    
  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, eventId: Int) = {
    eventStates.get(mapName).map { stateMapForCurMap =>
      stateMapForCurMap.get(eventId) getOrElse 0
    } getOrElse 0
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = {
    eventStates.getOrElseUpdate(
      mapName, 
      new MutableHashMap[Int, Int] with MutableSynchronizedMap[Int, Int])
      .update(eventId, newState)
  }
}