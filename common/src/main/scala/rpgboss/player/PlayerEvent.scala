package rpgboss.player

import rpgboss.model._

class PlayerEvent(game: MyGame) 
  extends Event(game: MyGame)
  with MoveInputHandler
{
  // Add input handling
  game.inputs.prepend(this)
  
  override def update(delta: Float) = {
    import MyKeys._
    
    var playerMoving = false
    var vx = 0f
    var vy = 0f
    
    val baseSpeed = 3.0f // tiles per second
    
    val projSpeed =
      if((isActive(Left) || isActive(Right)) &&
         (isActive(Up) || isActive(Down))) {
        (baseSpeed/math.sqrt(2.0)).toFloat
      } else {
        baseSpeed.toFloat
      }
    
    if(isActive(Left)) {
      playerMoving = true
      vx = -projSpeed
      dir = SpriteSpec.Directions.WEST
    } else if(isActive(Right)) {
      playerMoving = true
      vx = projSpeed
      dir = SpriteSpec.Directions.EAST
    }
    
    if(isActive(Up)) {
      playerMoving = true
      vy = -projSpeed
      dir = SpriteSpec.Directions.NORTH
    } else if(isActive(Down)) {
      playerMoving = true
      vy = projSpeed
      dir = SpriteSpec.Directions.SOUTH
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