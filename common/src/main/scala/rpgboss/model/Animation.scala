package rpgboss.model

case class AnimationEffect(frame: Int = 0, soundOpt: Option[SoundSpec] = None)

case class Animation(
  var name: String = "Animation", 
  var effects: Array[AnimationEffect] = Array()) extends HasName