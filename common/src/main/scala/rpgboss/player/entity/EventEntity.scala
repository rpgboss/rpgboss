package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.event._
import rpgboss.model.resource._
import rpgboss.player._
import rpgboss.model.SpriteSpec
import scala.concurrent.Promise
import scala.collection.mutable.Subscriber
import scala.collection.script.Message
import rpgboss.lib.Utils
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.utils.Disposable

case class EventScriptInterface(mapName: String, id: Int)

class EventEntity(
    project: Project,
    persistent: PersistentState,
    scriptInterface: ScriptInterface,
    scriptFactory: ScriptThreadFactory,
    spritesets: Map[String, Spriteset],
    mapAndAssetsOption: Option[MapAndAssets],
    eventEntities: collection.Map[Int, EventEntity],
    mapName: String,
    val mapEvent: RpgEvent)
  extends Entity(
      spritesets,
      mapAndAssetsOption,
      eventEntities,
      mapEvent.x,
      mapEvent.y)
  with LazyLogging
  with Disposable {

  def id = mapEvent.id

  override def zPriority: Float = {
    val zAdjustment = EventHeight(evtState.height) match {
      case EventHeight.UNDER => -10
      case EventHeight.SAME => 0
      case EventHeight.OVER => 10
    }
    return super.zPriority + zAdjustment
  }

  val states = if (mapEvent.isInstance) {
    val eventClass = project.data.enums.eventClasses(mapEvent.eventClassId)
    val eventClassStates = eventClass.states

    val bindCmds = mapEvent.params map { p =>
      SetLocalInt(p.localVariable, p)
    }

    // Bind local variables.
    eventClassStates.map(s => s.copy(cmds = bindCmds.toArray ++ s.cmds))
  } else {
    mapEvent.states
  }

  private var curThread: Option[ScriptThread] = None

  var evtStateIdx = 0

  def getX:Float = {
    return x
  }

  def getY:Float = {
    return y
  }

  /**
   * Maintain a cooldown to prevent events from firing too quickly.
   */
  private var _activateCooldown = 0.0f

  def getScriptInterface() = EventScriptInterface(mapName, id)

  def evtState: RpgEventState = states(evtStateIdx)
  def height = evtState.height

  val persistentListener =
    new Subscriber[PersistentStateUpdate, PersistentState#Pub] {
    def notify(pub: PersistentState#Pub, evt: PersistentStateUpdate) =
      evt match {
        case EventStateChange((mapName, id), _) => updateState()
        case IntChange(_, _) => updateState()
        case IntArrayChange(_) => updateState()
        case _ => Unit
      }
    persistent.subscribe(this)
  }

  def updateState(): Unit = {
    val proposedEvtStateIdx = persistent.getEventState(mapName, mapEvent.id)
    if (proposedEvtStateIdx < 0 || proposedEvtStateIdx >= states.length) {
      val clampedState =
        Utils.clamped(proposedEvtStateIdx, 0, states.length - 1)
      logger.error(
          "Event %s->%d doesn't have state %d. ".format(
              mapName, mapEvent.id, proposedEvtStateIdx) +
          "Clamping state to %d.".format(clampedState))
      persistent.setEventState(mapName, mapEvent.id, clampedState)
      return
    }

    evtStateIdx = proposedEvtStateIdx

    for (i <- 0 until states.length;
         if !states(i).conditions.isEmpty) {
      if (Condition.allConditionsTrue(states(i).conditions, scriptInterface)) {
        evtStateIdx = i
      }
    }

    return setSprite(evtState.sprite)
  }
  updateState()

  // Returns None if it's already running.
  def activate(activatorsDirection: Int): Option[Finishable] = {
    import SpriteSpec._

    if (curThread.isDefined)
      return None

    if (evtState.trigger == EventTrigger.ANYTOUCH.id ||
        evtState.trigger == EventTrigger.EVENTTOUCH.id ||
        evtState.trigger == EventTrigger.AUTORUN.id) {
      if (_activateCooldown > 0)
        return None
      else
        _activateCooldown = 0.2f
    }

    val origDir = dir
    if (!evtState.affixDirection && activatorsDirection != Directions.NONE)
      dir = Directions.opposite(activatorsDirection)

    val origState = evtStateIdx

    val startingMovesEnqueued = movesEnqueued

    curThread = Some(scriptFactory.runFromEventEntity(
      this,
      evtState,
      evtStateIdx,
      onFinish = Some(() => {
        val movedDuringScript = movesEnqueued != startingMovesEnqueued
        if (!evtState.affixDirection && activatorsDirection!= Directions.NONE &&
            !movedDuringScript && origState == evtStateIdx)
          dir = origDir
      })))

    return curThread
  }

  override def eventTouchCallback(touchedNpcs: Iterable[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.EVENTTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }

  override def update(delta: Float) = {
    super.update(delta)

    if (curThread.map(_.isFinished).getOrElse(false))
      curThread = None

    if (_activateCooldown > 0)
      _activateCooldown -= delta

    if (evtState.trigger == EventTrigger.AUTORUN.id &&
        _activateCooldown <= 0) {
      activate(SpriteSpec.Directions.NONE)
    }
  }

  override def dispose() = {
    // Kill any outstanding threads
    curThread.map(_.stop())
  }
}