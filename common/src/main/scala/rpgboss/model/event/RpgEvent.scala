package rpgboss.model.event

import rpgboss.model._

object EventTrigger extends RpgEnum {
  val NONE        = Value(0, "None")
  val BUTTON      = Value(1, "Button")
  val PLAYERTOUCH = Value(2, "Player Touch")
  val EVENTTOUCH  = Value(3, "Event Touch")
  val ANYTOUCH    = Value(4, "Any Touch")
  
  def default = BUTTON
}

object EventHeight extends RpgEnum {
  val UNDER = Value(0, "Under player")
  val SAME  = Value(1, "Same level as player")
  val OVER  = Value(2, "Always on top of player")
  
  def default = UNDER
}

import EventTrigger._

/**
 * Guaranteed to have at least one state
 */
case class RpgEvent(
    name: String, 
    x: Float, 
    y: Float, 
    states: Array[RpgEventState] = Array(RpgEventState()))

object RpgEvent {
  def blank(idFromMap: Int, x: Float, y: Float) = 
    RpgEvent("Event%05d".format(idFromMap), x, y, Array(RpgEventState()))
}

/**
 * cmds guaranteed to be of at least length 1 and end with an EndOfScript()
 */
case class RpgEventState(
    sprite: Option[SpriteSpec] = None, 
    trigger: Int = EventTrigger.BUTTON.id,
    height: Int = EventHeight.UNDER.id,
    cmds: Array[EventCmd] = RpgEventState.defaultCmds)

object RpgEventState {
  def defaultCmds: Array[EventCmd] = Array(EndOfScript())
}