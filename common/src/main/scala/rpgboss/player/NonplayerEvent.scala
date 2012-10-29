package rpgboss.player

import rpgboss.model.event.RpgEvent

class NonplayerEvent(game: MyGame, mapEvent: RpgEvent) 
  extends EventEntity(
      game, 
      mapEvent.x, 
      mapEvent.y) 
{
  setSprite(mapEvent.states.head.sprite)
}