package rpgboss.model.event

import rpgboss.model._

object EventTrigger extends RpgEnum {
  val NONE = Value(0, "None")
  val BUTTON = Value(1, "Button")
  val PLAYERTOUCH = Value(2, "Player Touch")
  val EVENTTOUCH = Value(3, "Event Touch")
  val ANYTOUCH = Value(4, "Any Touch")

  def default = BUTTON
}

object EventHeight extends RpgEnum {
  val UNDER = Value(0, "Under player")
  val SAME = Value(1, "Same level as player")
  val OVER = Value(2, "Always on top of player")

  def default = UNDER
}

import EventTrigger._

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
  var params: Map[String, EventParameter[_]] = Map()) {
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
 * @param   sameAppearanceAsPrevState   If true, takes on the height and sprite
 *                                      of the preceding state.
 * @param   runOnceThenIncrementState   If true, increments the state after
 *                                      running the commands.
 */
case class RpgEventState(
  var switchCondition: Option[EventCondition] = None,
  var trigger: Int = EventTrigger.BUTTON.id,
  var sameAppearanceAsPrevState: Boolean = true,
  var sprite: Option[SpriteSpec] = None,
  var height: Int = EventHeight.UNDER.id,
  var cmds: Array[EventCmd] = RpgEventState.defaultCmds,
  var runOnceThenIncrementState: Boolean = false) {

  def getFreeVariables() = {
    // All variables are free right now since there's exposed EventCmd to
    // bind them (for now).
    cmds.flatMap(_.getParameters().filter(
        _.valueTypeId == EventParameterValueType.LocalVariable.id))
  }
}

object RpgEventState {
  def defaultCmds: Array[EventCmd] = Array()
}

case class EventCondition(
  globalVariableConditions: Array[EventGlobalVariableCondition],
  itemCondition: Option[EventItemCondition],
  partyMemberCondition: Option[EventPartyMemberCondition])

case class EventGlobalVariableCondition(globalName: String, value: Int)
case class EventItemCondition(item: Int)
case class EventPartyMemberCondition(partyMember: Int)