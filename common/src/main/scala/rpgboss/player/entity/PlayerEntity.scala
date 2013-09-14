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

  override def update(delta: Float) = {
    import MyKeys._

    var playerMoving = false
    var vx = 0f
    var vy = 0f

    val speed = 3.0f // tiles per second

    if (isActive(Up)) {
      playerMoving = true
      vy = -speed
      dir = SpriteSpec.Directions.NORTH
    } else if (isActive(Down)) {
      playerMoving = true
      vy = speed
      dir = SpriteSpec.Directions.SOUTH
    }

    if (isActive(Left)) {
      playerMoving = true
      vx = -speed
      dir = SpriteSpec.Directions.WEST
    } else if (isActive(Right)) {
      playerMoving = true
      vx = speed
      dir = SpriteSpec.Directions.EAST
    }

    setMoving(playerMoving, vx, vy)

    // Do the basic event update stuff, including the actual moving
    super.update(delta)

    // Get the direction unit vector
    val (ux, uy) = dir match {
      case SpriteSpec.Directions.NORTH => (0f, -1f)
      case SpriteSpec.Directions.SOUTH => (0f, 1f)
      case SpriteSpec.Directions.EAST => (1f, 0f)
      case SpriteSpec.Directions.WEST => (-1f, 0f)
    }

    // Handle BUTTON interaction
    if (isActive(OK)) {
      val checkDist = 0.3f // Distance to check for a collision
      val activatedEvts =
        getAllEventTouches(ux * checkDist, uy * checkDist)
          .filter(_.evtState.trigger == EventTrigger.BUTTON.id)

      if (!activatedEvts.isEmpty) {
        val closestEvt =
          activatedEvts.minBy(e => math.abs(e.x - x) + math.abs(e.y - y))

        closestEvt.activate(dir)
      }
    } else if (isActive(Cancel)) {
      if (!menuActive) {
        menuActive = true
        ScriptThread.fromFile(
          game,
          "menu.js",
          "menu()",
          Some(() => menuActive = false)).run()
      }
    }
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