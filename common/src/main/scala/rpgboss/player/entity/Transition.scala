package rpgboss.player.entity

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
    val wEnd = age / duration
    val wStart = 1f - wEnd
    wStart * startAlpha + wEnd * endAlpha
  }
}