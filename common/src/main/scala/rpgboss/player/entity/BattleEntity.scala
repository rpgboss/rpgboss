package rpgboss.player.entity

import rpgboss.model._
import rpgboss.player._

class BattleEntity(
  game: MyGame,
  x: Float,
  y: Float,
  sprite: SpriteSpec)
  extends Entity(game, x, y, SpriteSpec.Directions.WEST, Some(sprite)) {
  override def screenSpace = true
}