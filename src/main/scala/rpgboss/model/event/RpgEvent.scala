package rpgboss.model.event

import rpgboss.model._

object EventTrigger extends Enumeration {
  type EventTrigger = Value
  val Button, Touch = Value 
}

import EventTrigger._

case class RpgEvent(name: String, x: Int, y: Int, states: Array[RpgEventState])

object RpgEvent {
  def blank(idFromMap: Int, x: Int, y: Int) = 
    RpgEvent("Event%05d".format(idFromMap), x, y, Array(RpgEventState()))
}

case class RpgEventState(
    sprite: Option[SpriteSpec] = None, 
    trigger: EventTrigger = Button,
    cmds: Array[EventCmd] = Array.empty)  