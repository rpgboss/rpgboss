package rpgboss.model

import com.badlogic.gdx.graphics.Color
import rpgboss.lib.TweenUtils

object AnimationFlashType extends RpgEnum{
  val Target = Value(0, "Target")
  val Screen = Value(1, "Screen")

  def default = Target
}

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

case class AnimationFlash(
  var startTime: Float = 0.0f,
  var endTime: Float = 1.0f,
  var color: ColorSpec = ColorSpec(),
  var flashTypeId: Int = AnimationFlashType.default.id) {
  def within(time: Float) = {
    time >= startTime && time < endTime
  }

  def duration = endTime - startTime

  def currentColor(time: Float) = {
    assert(time >= startTime)
    assert(time < endTime)
    // Linearly rising to max halfway through, then fading to zero.
    val firstHalf = (time - startTime) < (duration / 2)
    val percentMax =
      if (firstHalf)
        (time - startTime) / (duration / 2)
      else
        (endTime - time) / (duration / 2)
    new Color(color.r, color.g, color.b, percentMax * color.a)
  }
}

case class AnimationSound(
  var time: Float = 0.0f,
  var sound: SoundSpec = SoundSpec())

case class Animation(
  var name: String = "",
  var visuals: Array[AnimationVisual] = Array(),
  var sounds: Array[AnimationSound] = Array(),
  var flashes: Array[AnimationFlash] = Array()) extends HasName {

  def totalTime = {
    val visualsMaxTime =
      if (visuals.isEmpty) 0f else visuals.map(_.end.time).max
    val soundsMaxTime =
      if (sounds.isEmpty) 0f else sounds.map(_.time).max
    val flashesMaxTime =
      if (flashes.isEmpty) 0f else flashes.map(_.endTime).max

    math.max(visualsMaxTime, soundsMaxTime)
  }
}