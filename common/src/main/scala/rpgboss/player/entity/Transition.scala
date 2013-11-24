package rpgboss.player.entity

case class Transition(startAlpha: Float, endAlpha: Float, durationMs: Int) {
  val birthTime = System.currentTimeMillis()
  def age = (System.currentTimeMillis() - birthTime)
  def done = age > durationMs
  def curAlpha = if (done) endAlpha else {
    val wEnd = age.toDouble / durationMs
    val wStart = 1f - wEnd
    wStart * startAlpha + wEnd * endAlpha
  }
}