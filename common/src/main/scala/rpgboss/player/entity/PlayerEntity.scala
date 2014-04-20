package rpgboss.player.entity

import com.badlogic.gdx.utils.{Timer => GdxTimer}
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.player._
import rpgboss.player.entity._
import MyKeys.Down
import MyKeys.Left
import MyKeys.Right
import MyKeys.Up

class PlayerEntity(game: RpgGame, mapScreen: MapScreen)
  extends Entity(game: RpgGame)
  with PlayerInputHandler {
  assume(game != null)
  assume(mapScreen != null)

  // Add input handling
  mapScreen.inputs.prepend(this)

  var mapName: Option[String] = None
  var menuActive = false
  var currentMoveQueueItem: MutateQueueItem[Entity] = null
  speed = 4f // player should be faster

  // Set to a large number, as we expect to cancel this move when we lift button
  val moveSize = 1000f

  def changeFacingDirection() = {
    if (keyIsActive(Left))
      enqueueMove(EntityFaceDirection(SpriteSpec.Directions.WEST))
    else if (keyIsActive(Right))
      enqueueMove(EntityFaceDirection(SpriteSpec.Directions.EAST))
    else if (keyIsActive(Up))
      enqueueMove(EntityFaceDirection(SpriteSpec.Directions.NORTH))
    else if (keyIsActive(Down))
      enqueueMove(EntityFaceDirection(SpriteSpec.Directions.SOUTH))
  }

  def refreshPlayerMoveQueue() = {
    if (currentMoveQueueItem != null) {
      if (!currentMoveQueueItem.isDone())
        currentMoveQueueItem.finish()
      currentMoveQueueItem = null
    }

    if (!menuActive) {
      var totalDx = 0f
      var totalDy = 0f

      if (keyIsActive(Left))
        totalDx -= moveSize
      else if (keyIsActive(Right))
        totalDx += moveSize

      if (keyIsActive(Up))
        totalDy -= moveSize
      else if (keyIsActive(Down))
        totalDy += moveSize

      if (totalDx != 0f || totalDy != 0f) {
        val move = EntityMove(totalDx, totalDy)
        enqueueMove(move)
        currentMoveQueueItem = move
      }
    }
  }

  override def keyActivated(key: Int) = {
    game.logger.info("keyActivated: " + key.toString)
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
          mapScreen.scriptInterface,
          "menu.js",
          "menu()",
          Some(() => {
            menuActive = false
            refreshPlayerMoveQueue()
          })).run()
      }
    } else {
      changeFacingDirection()
      refreshPlayerMoveQueue()
    }
  }

  override def keyDeactivated(key: Int) = {
    refreshPlayerMoveQueue()

    // When the user has two buttons depressed to move diagonally, and then
    // lifts both, we detect is as two separate events. This can cause the
    // player sprite to change direction right as it stops moving, looking bad.
    // This delay to the direction change prevents that because
    // |changeFacingDirection()| does nothing when no keys are depressed.
    GdxTimer.schedule(
      new GdxTimer.Task {
        def run() = {
          changeFacingDirection()
          refreshPlayerMoveQueue()
        }
      },
      0.05f /* delaySeconds */)
  }

  override def eventTouchCallback(touchedNpcs: Iterable[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.PLAYERTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }

  // NOTE: this is never called... which may or may not be okay haha
  def dispose() = {
    mapScreen.inputs.remove(PlayerEntity.this)
  }

}