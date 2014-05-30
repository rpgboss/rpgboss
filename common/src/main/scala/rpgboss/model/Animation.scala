package rpgboss.model

case class AnimationEffect(frame: Int, sound: SoundSpec)

case class Animation(
  name: String = "Animation", 
  effects: Array[AnimationEffect] = Array()) extends HasName