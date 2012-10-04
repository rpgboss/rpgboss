package rpgboss.model.event

import rpgboss.model._

object EventTrigger extends Enumeration {
  type EventTrigger = Value
  val Button, Touch = Value 
}

import EventTrigger._

case class RpgEvent(loc: MapLoc, states: Array[RpgEventState])

case class RpgEventState(
    sprite: SpriteSpec, 
    trigger: EventTrigger,
    cmds: Array[EventCmd])