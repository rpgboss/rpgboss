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
 * Guaranteed to have at least one state
 */
case class RpgEvent(
  id: Int,
  var name: String,
  var x: Float,
  var y: Float,
  var states: Array[RpgEventState],
  var params: Map[String, EventParameter])

object RpgEvent {
  def blank(idFromMap: Int, x: Float, y: Float) =
    RpgEvent(idFromMap, "Event%05d".format(idFromMap), x, y,
             Array(RpgEventState()), Map())
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
  var runOnceThenIncrementState: Boolean = false)

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