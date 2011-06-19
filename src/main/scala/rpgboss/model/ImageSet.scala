package rpgboss.model

import rpgboss.lib._

case class Tileset(owner: String, game: String, name: String) {
  def resourceType = "tileset"
}
