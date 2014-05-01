package rpgboss.player
import collection.mutable._
import collection.mutable.{HashMap => MutableHashMap}
import rpgboss.lib.ThreadChecked

trait PersistentStateUpdate
case class IntChange(key: String, value: Int) extends PersistentStateUpdate
case class EventStateChange(key: (String, Int), value: Int)
  extends PersistentStateUpdate

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState
  extends ThreadChecked
  with Publisher[PersistentStateUpdate] {
  private val globalInts = new MutableHashMap[String, Int]
  private val intArrays = new MutableHashMap[String, Array[Int]]
  private val stringArrays = new MutableHashMap[String, Array[String]]

  // mapName->eventId->state
  val eventStates =
    new MutableHashMap[(String, Int), Int]
    with ObservableMap[(String, Int), Int]

  // TODO: save player location
  def setInt(key: String, value: Int) = {
    assert(onValidThread())
    globalInts.update(key, value)
    publish(IntChange(key, value))
  }
  def getInt(key: String) = {
    assert(onValidThread())
    globalInts.get(key).getOrElse(-1)
  }

  def getIntArray(key: String) = {
    assert(onValidThread())
    intArrays.get(key).getOrElse(new Array[Int](0))
  }

  def setIntArray(key: String, value: Seq[Int]) = {
    assert(onValidThread())
    intArrays.update(key, value.toArray)
  }

  def getStringArray(key: String) = {
    assert(onValidThread())
    stringArrays.getOrElse(key, new Array[String](0))
  }

  def setStringArray(key: String, value: Seq[String]) = {
    assert(onValidThread())
    stringArrays.update(key, value.toArray)
  }

  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, eventId: Int) = {
    assert(onValidThread())
    eventStates.get((mapName, eventId)).getOrElse(0)
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = {
    assert(onValidThread())
    eventStates.update((mapName, eventId), newState)
  }
}