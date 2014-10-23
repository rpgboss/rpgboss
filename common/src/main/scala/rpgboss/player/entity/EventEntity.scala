package rpgboss.player.entity

import rpgboss.model.event._
import rpgboss.player._
import rpgboss.model.SpriteSpec
import scala.concurrent.Promise
import scala.collection.mutable.Subscriber
import scala.collection.script.Message

case class EventScriptInterface(id: Int)

class EventEntity(game: RpgGame, mapName: String, val mapEvent: RpgEvent)
  extends Entity(game, mapEvent.x, mapEvent.y) {

  def id = mapEvent.id

  private var curThread: Option[ScriptThread] = None

  var evtStateIdx = 0

  def getScriptInterface() = EventScriptInterface(id)

  def evtState = mapEvent.states(evtStateIdx)

  def height: Int = {
    for (i <- evtStateIdx to 1 by -1) {
      if (!mapEvent.states(i).sameAppearanceAsPrevState)
        return mapEvent.states(i).height
    }
    return mapEvent.states.head.height
  }

  val persistentListener =
    new Subscriber[PersistentStateUpdate, PersistentState#Pub] {
    def notify(pub: PersistentState#Pub, evt: PersistentStateUpdate) =
      evt match {
        case EventStateChange((mapName, id), _) => updateState()
        case _ => Unit
      }
    game.persistent.subscribe(this)
  }

  def updateState(): Unit = {
    evtStateIdx = game.persistent.getEventState(mapName, mapEvent.id)
    for (i <- evtStateIdx to 1 by -1) {
      if (!mapEvent.states(i).sameAppearanceAsPrevState)
        return setSprite(mapEvent.states(i).sprite)
    }
    return setSprite(mapEvent.states.head.sprite)
  }
  updateState()

  // Returns None if it's already running.
  def activate(activatorsDirection: Int =
                   SpriteSpec.Directions.NONE): Option[ScriptThread] = {
    import SpriteSpec._

    if (curThread.isDefined)
      return None

    val origDir = dir
    if (activatorsDirection != Directions.NONE)
      dir = Directions.opposite(activatorsDirection)

    val startingMovesEnqueued = movesEnqueued

    curThread = Some(ScriptThread.fromEventEntity(
      game,
      game.mapScreen,
      game.mapScreen.scriptInterface,
      this, evtStateIdx,
      onFinish = Some(() => {
        val movedDuringScript = movesEnqueued != startingMovesEnqueued
        if (activatorsDirection!= Directions.NONE && !movedDuringScript)
          dir = origDir
        curThread = None
      })))
    curThread.get.run()

    return curThread
  }

  override def eventTouchCallback(touchedNpcs: Iterable[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.EVENTTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }
}