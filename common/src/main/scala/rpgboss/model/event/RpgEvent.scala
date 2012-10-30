package rpgboss.model.event

import rpgboss.model._

object EventTrigger {
  val NoTrigger = 0
  val Button    = 1
  val Touch     = 2 
  
  val choices = Array("None", "Button", "Touch")
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
    trigger: Int = Button,
    cmds: Array[EventCmd] = RpgEventState.defaultCmds)

object RpgEventState {
  def defaultCmds: Array[EventCmd] = Array(EndOfScript())
}