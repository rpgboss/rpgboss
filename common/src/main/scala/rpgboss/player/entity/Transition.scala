package rpgboss.player.entity

import rpgboss.lib.TweenUtils
import scala.collection.mutable.ArrayBuffer

case class Transition(startAlpha: Float, endAlpha: Float, duration: Float) {
  private var age = 0.0f
  def update(delta: Float) = {
    assert(delta >= 0)
    age += delta

    if (done && !pendingClosures.isEmpty) {
      flushPendingClosures()
    }
  }

  def flushPendingClosures() = {
    pendingClosures.foreach(_())
    pendingClosures.clear()
  }

  def done = age >= duration
  def curAlpha = if (done) {
    endAlpha
  } else {
    val alpha = age / duration
    TweenUtils.tweenFloat(alpha, startAlpha, endAlpha)
  }

  val pendingClosures = ArrayBuffer[() => Unit]()
}