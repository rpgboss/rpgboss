package rpgboss.player.entity

import rpgboss.player.RpgGame
import rpgboss.model.event._
import rpgboss.player.Finishable
import com.typesafe.scalalogging.slf4j.LazyLogging

class VehicleEntity(game: RpgGame, vehicleId: Int)
  extends Entity(
      game.spritesets,
      game.mapScreen.mapAndAssetsOption,
      game.mapScreen.allEntities)
  with LazyLogging {
  override def height = EventHeight.SAME.id
  override def trigger = EventTrigger.BUTTON.id

  override def activate(activatorsDirection: Int): Option[Finishable] = {
    logger.debug("Activated vehicle id: %d".format(vehicleId))
    None
  }

  override def dispose() = {
  }

  setSprite(game.project.data.vehicles(vehicleId).sprite)
}