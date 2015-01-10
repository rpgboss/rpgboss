package rpgboss.model.event

import rpgboss.model._

import rpgboss.lib.Utils

object EventTrigger extends RpgEnum {
  val NONE = Value(0, "None")
  val BUTTON = Value(1, "Button")
  val PLAYERTOUCH = Value(2, "Touch by Player")
  val EVENTTOUCH = Value(3, "Touch by Other Event")
  val ANYTOUCH = Value(4, "Touch by Any")
  val AUTORUN = Value(5, "Autorun (parallel)")

  def default = BUTTON
}

object EventHeight extends RpgEnum {
  val UNDER = Value(0, "Under player")
  val SAME = Value(1, "Same level as player")
  val OVER = Value(2, "Always on top of player")

  def default = UNDER
}

/**
 * @param   states          Guaranteed to be size at least 1, unless this event
 *                          is an event instance, in which case size must be 0.
 * @param   eventClassId    -1 if normal event. Otherwise, the id of the event
 *                          class this is an instance of.
 * @param   params          Variables to bind for the event class.
 */
case class RpgEvent(
  id: Int = 0,
  var name: String = "",
  var x: Float = 0,
  var y: Float = 0,
  var states: Array[RpgEventState] = Array(RpgEventState()),
  var eventClassId: Int = -1,
  var params: Array[EventParameter[_]] = Array()) {
  def isInstance = eventClassId >= 0
}

object RpgEvent {
  def blank(idFromMap: Int, x: Float, y: Float) =
    RpgEvent(idFromMap, "Event%05d".format(idFromMap), x, y,
             Array(RpgEventState()))

  def blankInstance(idFromMap: Int, x: Float, y: Float) =
    RpgEvent(idFromMap, "Event%05d".format(idFromMap), x, y,
             Array.empty, 0)
}

/**
 * @param   cmds                        May be empty.
 * @param   runOnceThenIncrementState   If true, increments the state after
 *                                      running the commands.
 */
case class RpgEventState(
  var conditions: Array[Condition] = Array(),
  var sprite: Option[SpriteSpec] = None,
  var height: Int = EventHeight.UNDER.id,
  var affixDirection: Boolean = false,

  var trigger: Int = EventTrigger.BUTTON.id,
  var runOnceThenIncrementState: Boolean = false,

  var cmds: Array[EventCmd] = RpgEventState.defaultCmds) {

  def getFreeVariables() = {
    // All variables are free right now since there's exposed EventCmd to
    // bind them (for now).
    cmds.flatMap(_.getParameters().filter(
        _.valueTypeId == EventParameterValueType.LocalVariable.id))
  }

  def copyEssentials() = {
    val newState = RpgEventState()
    newState.sprite = sprite
    newState.height = height
    newState.affixDirection = affixDirection
    newState
  }
  def copyAll() = Utils.deepCopy(this)
}

object RpgEventState {
  def defaultCmds: Array[EventCmd] = Array()
}
