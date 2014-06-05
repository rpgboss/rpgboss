package rpgboss.model

case class AnimationEffect(frame: Int = 0, soundOpt: Option[SoundSpec])

case class Animation(
  var name: String = "Animation", 
  var effects: Array[shapeRendererAnimationEffect] = Array()) extends HasName