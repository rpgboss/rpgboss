package rpgboss.player.entity

import rpgboss.lib.TweenUtils

case class Transition(startAlpha: Float, endAlpha: Float, duration: Float) {
  private var age = 0.0f
  def update(delta: Float) = {
    assert(delta >= 0)
    age += delta
  }
  def done = age >= duration
  def curAlpha = if (done) {
    endAlpha
  } else {
    val alpha = age / duration
    TweenUtils.tweenFloat(alpha, startAlpha, endAlpha)
  }
}