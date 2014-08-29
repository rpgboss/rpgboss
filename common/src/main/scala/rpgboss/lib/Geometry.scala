package rpgboss.lib

/**
 * @param   x   Refers to left of the rectangle.
 * @param   y   Refers to top of the rectangle.
 */
trait BoxLike {
  def x: Float
  def y: Float
  def w: Float
  def h: Float

  def xCenter = x + w / 2
  def yCenter = y + h / 2

  def getRect() = Rect(x, y, w, h)
}

/**
 * @param   x   Refers to left of the rectangle.
 * @param   y   Refers to top of the rectangle.
 */
case class Rect(x: Float, y: Float, w: Float, h: Float) extends BoxLike