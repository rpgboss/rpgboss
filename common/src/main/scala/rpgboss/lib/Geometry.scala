package rpgboss.lib

/**
 * @param   x   Refers to left of the rectangle.
 * @param   y   Refers to top of the rectangle.
 */
trait BoxLike {
  def x: Int
  def y: Int
  def w: Int
  def h: Int

  def xCenter = x + w / 2
  def yCenter = y + h / 2

  def getIntRect() = IntRect(x, y, w, h)
}

/**
 * @param   x   Refers to left of the rectangle.
 * @param   y   Refers to top of the rectangle.
 */
case class IntRect(x: Int, y: Int, w: Int, h: Int) extends BoxLike