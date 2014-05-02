package rpgboss.model

case class AnimationTileSpec(name: String, xTile: Int, yTile: Int)

case class AnimationFrameTiles(tiles: Array[AnimationTileSpec] = Array())

case class FlashColor(hue: Int, sat: Int, value: Int)

case class AnimationEffect(frame: Int, flash: FlashColor, sound: SoundSpec)

case class Animation(
  name: String = "Animation",
  framesTiles: Array[AnimationFrameTiles] = Array(AnimationFrameTiles()), 
  effects: Array[AnimationEffect] = Array()) extends HasName