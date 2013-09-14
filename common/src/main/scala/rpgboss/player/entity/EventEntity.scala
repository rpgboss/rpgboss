package rpgboss.player.entity

import rpgboss.model.event._
import rpgboss.player.MyGame
import rpgboss.player.ScriptThread
import rpgboss.model.SpriteSpec

class EventEntity(game: MyGame, val mapEvent: RpgEvent)
  extends Entity(
    game,
    mapEvent.x,
    mapEvent.y) {
  private var curThread: ScriptThread = null

  var evtStateIdx = 0

  def evtState = mapEvent.states(evtStateIdx)

  def updateState() = {
    evtStateIdx = game.state.getEvtState(mapEvent.name)
    setSprite(evtState.sprite)
  }
  updateState()

  def activate(activatorsDirection: Int) = {
    if (curThread == null || curThread.isFinished) {
      import SpriteSpec.Directions._
      val origDir = dir
      dir = activatorsDirection match {
        case EAST => WEST
        case WEST => EAST
        case NORTH => SOUTH
        case SOUTH => NORTH
      }

      curThread = ScriptThread.fromEventEntity(
        game,
        mapEvent, this, evtStateIdx,
        onFinishSyncCallback = Some(() => {
          dir = origDir
        }))
      curThread.run()
    }
  }

  def eventTouchCallback(touchedNpcs: List[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.EVENTTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }
}