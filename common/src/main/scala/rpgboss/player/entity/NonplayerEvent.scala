package rpgboss.player.entity

import rpgboss.model.event._
import rpgboss.player.MyGame
import rpgboss.player.entity.EventEntity
import rpgboss.player.ScriptThread
import rpgboss.model.SpriteSpec

class NonplayerEvent(game: MyGame, mapEvent: RpgEvent) 
  extends EventEntity(
      game, 
      mapEvent.x, 
      mapEvent.y) 
{
  setSprite(mapEvent.states.head.sprite)
  
  var curThread: ScriptThread = null
  
  var evtState = 0
  
  def activate(activatorsDirection: Int) = {
    if(curThread == null || curThread.isFinished) {
      import SpriteSpec.Directions._
      val origDir = dir
      dir = activatorsDirection match {
        case EAST  => WEST
        case WEST  => EAST
        case NORTH => SOUTH
        case SOUTH => NORTH
      }      
      
      curThread = ScriptThread.fromEvent(
          game, 
          mapEvent, evtState, 
          onFinishSyncCallback = Some(() => {
            dir = origDir
          }))
      curThread.run()
    }
  }
}