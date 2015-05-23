package rpgboss.player.entity

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.{Timer => GdxTimer}
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.player._
import rpgboss.player.entity._
import MyKeys.Down
import MyKeys.Left
import MyKeys.Right
import MyKeys.Up
import scala.collection.mutable.Subscriber

class PlayerEntity(game: RpgGame, mapScreen: MapScreen)
  extends Entity(
      game.spritesets,
      game.mapScreen.mapAndAssetsOption,
      game.mapScreen.allEntities)
  with PlayerInputHandler
  with HasScriptConstants {
  assume(game != null)
  assume(mapScreen != null)

  override def height = EventHeight.SAME.id
  override def trigger = EventTrigger.NONE.id

  override def isPlayer = true

  var _collisionOn = true
  override def collisionOn = _collisionOn
  private var _inVehicleId: Int = -1
  override def inVehicle = _inVehicleId >= 0
  override def inVehicleId = _inVehicleId

  // Add input handling
  mapScreen.inputs.prepend(this)

  var mapName: Option[String] = None
  var menuActive = false
  var currentMoveQueueItem: MutateQueueItem[Entity] = null
  speed = 4f // player should be faster

  // Set to a large number, as we expect to cancel this move when we lift button
  val moveSize = 1000f

  def updateSprite() = {
    val partyArray = game.persistent.getIntArray(PARTY)
    if (partyArray.length > 0) {
      val spritespec = game.project.data.enums.characters(partyArray(0)).sprite
      setSprite(spritespec)
    } else {
      setSprite(None)
    }
  }

  def setInVehicle(inVehicle: Boolean, vehicleId: Int): Unit = {
    if (inVehicle) {
      _inVehicleId = vehicleId
      setSprite(None)
    } else {
      _inVehicleId = -1
      updateSprite()
    }
  }

  val persistentListener =
    new Subscriber[PersistentStateUpdate, PersistentState#Pub] {
    def notify(pub: PersistentState#Pub, evt: PersistentStateUpdate) =
      evt match {
        case IntArrayChange(_) => updateSprite()
        case _ => Unit
      }
    game.persistent.subscribe(this)
  }

  def getScriptInterface() = EventScriptInterface(mapName.getOrElse(""), -1)

  def processMoveKeys(): Unit = {
    if (currentMoveQueueItem != null) {
      if (!currentMoveQueueItem.isFinished)
        currentMoveQueueItem.finish()
      currentMoveQueueItem = null
    }

    val movementLocked =
      game.persistent.getInt(ScriptInterfaceConstants.PLAYER_MOVEMENT_LOCKS) > 0
    if (!menuActive && !movementLocked) {
      // Change direction
      if (keyIsActive(Left))
        enqueueMove(EntityFaceDirection(SpriteSpec.Directions.WEST))
      else if (keyIsActive(Right))
        enqueueMove(EntityFaceDirection(SpriteSpec.Directions.EAST))
      else if (keyIsActive(Up))
        enqueueMove(EntityFaceDirection(SpriteSpec.Directions.NORTH))
      else if (keyIsActive(Down))
        enqueueMove(EntityFaceDirection(SpriteSpec.Directions.SOUTH))

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
        val move = EntityMove(new Vector2(totalDx, totalDy))
        enqueueMove(move)
        currentMoveQueueItem = move
      }
    }
  }

  override def keyActivated(key: Int) = {
    import MyKeys._
    // Handle BUTTON interaction
    if (key == OK) {
      if (inVehicle) {
        Some(game.mapScreen.scriptFactory.runFunction(
            "game.exitVehicle();"))
      } else {
        val (ux, uy) = getDirectionUnitVector()
        val checkDist = 0.4f // Distance to check for key activation

        val activatedEvts =
          getAllEventTouches(ux * checkDist, uy * checkDist)
            .filter(_.trigger == EventTrigger.BUTTON.id)

        if (!activatedEvts.isEmpty) {
          closest(activatedEvts, ux, uy).activate(dir)
        }
      }
    } else if (key == Cancel && !inVehicle) {
      if (!menuActive) {
        menuActive = true
        mapScreen.scriptFactory.runFromFile("sys/menu.js",
          "menu()",
          Some(() => {
            menuActive = false
            processMoveKeys()
          }))
      }
    } else {
      processMoveKeys()
    }
  }

  override def keyDeactivated(key: Int) = {
    processMoveKeys()

    // When the user has two buttons depressed to move diagonally, and then
    // lifts both, we detect is as two separate events. This can cause the
    // player sprite to change direction right as it stops moving, looking bad.
    // This delay to the direction change prevents that because
    // we don't change direction does nothing when no keys are depressed.
    GdxTimer.schedule(
      new GdxTimer.Task {
        def run() = {
          processMoveKeys()
        }
      },
      0.05f /* delaySeconds */)
  }

  override def touchEntities(entities: Iterable[Entity]) = {
    val activatedEvts =
      entities.filter(e =>
        e.trigger == EventTrigger.PLAYERTOUCH.id ||
          e.trigger == EventTrigger.ANYTOUCH.id)

    if (!activatedEvts.isEmpty)
      closest(activatedEvts).activate(dir)
  }

  // NOTE: this is never called... which may or may not be okay haha
  def dispose() = {
    mapScreen.inputs.remove(PlayerEntity.this)
  }

}