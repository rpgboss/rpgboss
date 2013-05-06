package rpgboss.player

/**
 * The part of the game state that should persist through save and load cycles
 */
class PersistentState {
  // Contains current map and the camera position
  val cameraLoc = new MutableMapLoc()

  // Should only be accessed on the Gdx thread, so no synchronization needed
  val pictures = Array.fill[Option[PictureInfo]](32)(None)

  // Should only be accessed on gdx thread
  private val globalVariables = collection.mutable.Map[String, Int]()

  // mapName->evtName->variableName
  private val eventStates =
    collection.mutable.Map[String, collection.mutable.Map[String, Int]]()

  // TODO: save player location

  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, evtName: String) = {
    eventStates.get(mapName).map { stateMapForCurMap =>
      stateMapForCurMap.get(evtName) getOrElse 0
    } getOrElse 0
  }

  def setEventState(mapName: String, eventName: String, newState: Int) = {
    eventStates.getOrElseUpdate(mapName, collection.mutable.Map[String, Int]())
      .update(eventName, newState)
  }
}