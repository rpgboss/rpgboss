package rpgboss.model

case class Vehicle(
  var name: String = "",
  var sprite: Option[SpriteSpec] = None,
  var canFly: Boolean = false) extends HasName