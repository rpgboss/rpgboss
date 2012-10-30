package rpgboss.player.entity

import rpgboss.model._
import rpgboss.player._
import rpgboss.player.entity.EventEntity

import MyKeys.Down
import MyKeys.Left
import MyKeys.Right
import MyKeys.Up

class PlayerEvent(game: MyGame) 
  extends EventEntity(game: MyGame)
  with MoveInputHandler
{
  // Add input handling
  game.inputs.prepend(this)
  
  override def update(delta: Float) = {
    import MyKeys._
    
    var playerMoving = false
    var vx = 0f
    var vy = 0f
    
    val speed = 3.0f // tiles per second
    
    if(isActive(Up)) {
      playerMoving = true
      vy = -speed
      dir = SpriteSpec.Directions.NORTH
    } else if(isActive(Down)) {
      playerMoving = true
      vy = speed
      dir = SpriteSpec.Directions.SOUTH
    }
    
    if(isActive(Left)) {
      playerMoving = true
      vx = -speed
      dir = SpriteSpec.Directions.WEST
    } else if(isActive(Right)) {
      playerMoving = true
      vx = speed
      dir = SpriteSpec.Directions.EAST
    }
    
    setMoving(playerMoving, vx, vy)
    
    // Do the basic event update stuff, including the actual moving
    super.update(delta)
  }
  
  // NOTE: this is never called... which may or may not be okay haha
  def dispose() = {
    game.inputs.remove(this)
  }

}