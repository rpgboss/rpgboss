package rpgboss.player

class ShakeManager {
  var _isShaking = false
  var _timeInShake = 0f

  var _xAmplitude = 0f
  var _yAmplitude = 0f
  var _frequency = 0f
  var _duration = 0f

  var _xDisplacement = 0f
  var _yDisplacement = 0f

  def xDisplacement = _xDisplacement
  def yDisplacement = _yDisplacement

  def update(delta: Float): Unit = {
    if (!_isShaking)
      return

    _timeInShake += delta
    if (_timeInShake > _duration) {
      _isShaking = false
      _xDisplacement = 0
      _yDisplacement = 0
      return
    }

    val modulatingFactor =
      math.sin(_timeInShake * _frequency * math.Pi * 2).toFloat
    _xDisplacement = _xAmplitude * modulatingFactor
    _yDisplacement = _yAmplitude * modulatingFactor
  }

  def startShake(xAmplitude: Float, yAmplitude: Float, frequency: Float,
      duration: Float) = {
    _isShaking = true
    _timeInShake = 0f

    _xAmplitude = xAmplitude
    _yAmplitude = yAmplitude
    _frequency = frequency

    // Duration should be a multiple of 1/(2*frequency) seconds.
    val halfPeriod = 1 / (2 * frequency)
    _duration = (duration / halfPeriod).round * halfPeriod
  }
}