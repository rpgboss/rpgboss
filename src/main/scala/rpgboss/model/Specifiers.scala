package rpgboss.model

/**
 * These locations are given with the top-left of the map being at (0, 0).
 * This means that the center of the tiles are actually at 0.5 intervals. 
 */
case class MapLoc(map: String, x: Float, y: Float)

case class SpriteSpec(spriteset: String, spriteindex: Int) 

case class Character(defaultName: String, sprite: SpriteSpec)