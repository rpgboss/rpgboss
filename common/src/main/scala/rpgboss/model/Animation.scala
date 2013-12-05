package rpgboss.model

case class AnimationTileSpec(name: String, xTile: Int, yTile: Int)

case class AnimationFrameTiles(tiles: Seq[AnimationTileSpec] = Seq())

case class FlashColor(hue: Int, sat: Int, value: Int)

case class AnimationEffect(frame: Int, flash: FlashColor, sound: SoundSpec)

case class Animation(
  name: String = "Animation",
  framesTiles: Seq[AnimationFrameTiles] = Seq(AnimationFrameTiles()), 
  effects: Seq[AnimationEffect] = Seq()) extends HasName