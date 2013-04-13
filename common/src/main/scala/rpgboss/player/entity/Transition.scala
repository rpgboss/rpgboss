package rpgboss.player.entity

case class Transition(startAlpha: Float, endAlpha: Float, durationMs: Int) {
  val birthTime = System.currentTimeMillis()
  def age = (System.currentTimeMillis() - birthTime)
  def done = age > durationMs
  def curAlpha = if (done) endAlpha else {
    val w1 = age.toFloat / durationMs
    val w2 = 1f - w1
    w1 * startAlpha + w2 * endAlpha
  }
}