package rpgboss.player

/**
 * The part of the game state that should persist through save and load cycles
 */
class PersistentState {
  // Contains current map and the camera position
  val cameraLoc = new MutableMapLoc()
  
  // Should only be accessed on the Gdx thread, so no synchronization needed
  val pictures = new Array[PictureInfo](32)
  
  // Should only be accessed on gdx thread
  // mapName->(evtName->stateIdx)
  private var evtStates = Map[String, Map[String, Int]]()
  
  // TODO: save player location
    
  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEvtState(mapName: String, evtName: String) = {
    evtStates.get(mapName).map { stateMapForCurMap =>
      stateMapForCurMap.get(evtName) getOrElse 0
    } getOrElse 0
  }
  
  def setEvtState(mapName: String, evtName: String, newState: Int) = {
    // Initialize a submap if one doesn't exist
    val evtStatesForCurMap = evtStates.getOrElse(cameraLoc.map, Map())
    
    val updatedEvtStatesForCurMap = 
      evtStatesForCurMap.updated(evtName, newState)
    
    evtStates = evtStates.updated(cameraLoc.map, updatedEvtStatesForCurMap)
  }
}