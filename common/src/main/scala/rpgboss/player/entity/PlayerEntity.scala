package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.event._
import rpgboss.player._
import rpgboss.player.entity._
import MyKeys.Down
import MyKeys.Left
import MyKeys.Right
import MyKeys.Up

class PlayerEntity(game: MyGame)
  extends Entity(game: MyGame)
  with MoveInputHandler {
  // Add input handling
  game.inputs.prepend(PlayerEntity.this)

  var menuActive = false
  var currentMoveQueueItem: EntityMoveTrait = null
  
  // Set to a large number, as we expect to cancel this move when we lift button
  val moveSize = 1000f
  
  def refreshPlayerMoveQueue() = {
    if (currentMoveQueueItem != null && !currentMoveQueueItem.isDone())
      currentMoveQueueItem.finish()
    
    var totalDx = 0f
    var totalDy = 0f
    
    if (isActive(Left)) {
      enqueueMove(EntityFace(this, SpriteSpec.Directions.WEST))
      totalDx -= moveSize
    } else if (isActive(Right)) {
      enqueueMove(EntityFace(this, SpriteSpec.Directions.EAST))
      totalDx += moveSize
    }

    if (isActive(Up)) {
      enqueueMove(EntityFace(this, SpriteSpec.Directions.NORTH))
      totalDy -= moveSize
    } else if (isActive(Down)) {
      enqueueMove(EntityFace(this, SpriteSpec.Directions.SOUTH))
      totalDy += moveSize
    }

    if (totalDx != 0f || totalDy != 0f) {
      val move = EntityMove(this, totalDx, totalDy)
      enqueueMove(move)
      currentMoveQueueItem = move
    }
  }
    
  override def keyDown(key: Int) = {
    super.keyDown(key)
    
    import MyKeys._
    // Handle BUTTON interaction
    if (key == OK) {
      // Get the direction unit vector
      val (ux, uy) = dir match {
        case SpriteSpec.Directions.NORTH => (0f, -1f)
        case SpriteSpec.Directions.SOUTH => (0f, 1f)
        case SpriteSpec.Directions.EAST => (1f, 0f)
        case SpriteSpec.Directions.WEST => (-1f, 0f)
      }
      
      val checkDist = 0.3f // Distance to check for a collision
      val activatedEvts =
        getAllEventTouches(ux * checkDist, uy * checkDist)
          .filter(_.evtState.trigger == EventTrigger.BUTTON.id)

      if (!activatedEvts.isEmpty) {
        val closestEvt =
          activatedEvts.minBy(e => math.abs(e.x - x) + math.abs(e.y - y))

        closestEvt.activate(dir)
      }
    } else if (key == Cancel) {
      if (!menuActive) {
        menuActive = true
        ScriptThread.fromFile(
          game,
          "menu.js",
          "menu()",
          Some(() => menuActive = false)).run()
      } 
    } else {
      refreshPlayerMoveQueue()
    }
  }
  
  override def keyCancelled(key: Int) = {
    super.keyCancelled(key)
    
    import MyKeys._
    if (key == Left || key == Right || key == Up || key == Down)
      refreshPlayerMoveQueue()
  }
  
  override def update(delta: Float) = {

    // Do the basic event update stuff, including the actual moving
    super.update(delta)


  }

  def eventTouchCallback(touchedNpcs: List[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.PLAYERTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }

  // NOTE: this is never called... which may or may not be okay haha
  def dispose() = {
    game.inputs.remove(PlayerEntity.this)
  }

}