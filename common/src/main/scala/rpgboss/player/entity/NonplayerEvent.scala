package rpgboss.player.entity

import rpgboss.model.event._
import rpgboss.player.MyGame
import rpgboss.player.entity.EventEntity

class NonplayerEvent(game: MyGame, mapEvent: RpgEvent) 
  extends EventEntity(
      game, 
      mapEvent.x, 
      mapEvent.y) 
{
  setSprite(mapEvent.states.head.sprite)
}