package rpgboss.player.entity

import rpgboss.model._
import rpgboss.player._

class BattleEntity(game: MyGame)
  extends Entity(game: MyGame) {
  override def screenSpace = true
}