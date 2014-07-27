package rpgboss.model

case class AnimationKeyframe(
  var time: Float = 0.0f,
  var frameIndex: Int = 0,
  var x: Int = 0,
  var y: Int = 0)

case class AnimationVisual(
  var animationImage: String = "",
  var start: AnimationKeyframe = AnimationKeyframe(),
  var end: AnimationKeyframe = AnimationKeyframe()) {
  def within(time: Float) = {
    time >= start.time && time < end.time
  }
}

case class AnimationSound(
  var time: Float = 0.0f,
  var sound: SoundSpec = SoundSpec())

case class Animation(
  var name: String = "Animation",
  var visuals: Array[AnimationVisual] = Array(),
  var sounds: Array[AnimationSound] = Array()) extends HasName {

  def totalTime = {
    val visualsMaxTime =
      if (visuals.isEmpty) 0f else visuals.map(_.end.time).max
    val soundsMaxTime =
      if (sounds.isEmpty) 0f else sounds.map(_.time).max

    math.max(visualsMaxTime, soundsMaxTime)
  }
}