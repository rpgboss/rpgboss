package rpgboss.model

case class AnimationKeyframe(
  var frameIndex: Int = 0,
  var x: Int = 0,
  var y: Int = 0)

case class AnimationVisual(
  var startTime: Float = 0.0f,
  var endTime: Float = 1.0f,
  var animationName: String = "",
  var startFrame: AnimationKeyframe = AnimationKeyframe(),
  var endFrame: AnimationKeyframe = AnimationKeyframe())

case class AnimationSound(
  var time: Float = 0.0f,
  var sound: SoundSpec = SoundSpec())

case class Animation(
  var name: String = "Animation",
  var visuals: Array[AnimationVisual] = Array(),
  var sounds: Array[AnimationSound] = Array()) extends HasName