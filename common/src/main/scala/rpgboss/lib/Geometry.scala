package rpgboss.lib

/**
 * @param   x   Refers to center of the rectangle.
 * @param   y   Refers to center of the rectangle.
 */
trait BoxLike {
  def x: Float
  def y: Float
  def w: Float
  def h: Float

  def left = x - w / 2
  def top = y - h / 2
  def bot = y + h / 2

  def getRect() = Rect(x, y, w, h)
}

/**
 * @param   x   Refers to center of the rectangle.
 * @param   y   Refers to center of the rectangle.
 */
case class Rect(x: Float, y: Float, w: Float, h: Float) extends BoxLike

case class Size(w: Float, h: Float)

/**
 * @param   screenW   Should always be an integer, but treated as a float for
 *                    convenience.
 * @param   screenH   Should always be an integer, but treated as a float for
 *                    convenience.
 */
class LayoutProvider(screenW: Float, screenH: Float) {
  def proportional(xProportion: Float, yProportion: Float, w: Float, h: Float) =
    Rect(xProportion * screenW, yProportion * screenH, w, h)

  def north(w: Float, h: Float) =
    Rect(screenW / 2, h / 2, w, h)

  def south(w: Float, h: Float) =
    Rect(screenW / 2, screenH - h / 2, w, h)

  def east(w: Float, h: Float) =
    Rect(w / 2, screenH / 2, w, h)

  def west(w: Float, h: Float) =
    Rect(screenW - w /2, screenH / 2, w, h)

  def northEast(w: Float, h: Float) =
    Rect(screenW - w / 2, h / 2, w, h)

  def northWest(w: Float, h: Float) =
    Rect(w / 2, h / 2, w, h)

  def southEast(w: Float, h: Float) =
    Rect(screenW - w / 2, screenH - h / 2, w, h)

  def southWest(w: Float, h: Float) =
    Rect(w / 2, screenH - h / 2, w, h)

  def centered(w: Float, h: Float) =
    Rect(screenW / 2, screenH / 2, w, h)
}

class SizeProvider(screenW: Float, screenH: Float) {
  def fixed(w: Float, h: Float) = Size(w, h)

  /**
   * Fills whole screen. May cause some portions of image to be beyond the
   * screen borders.
   */
  def fill(w: Float, h: Float) = {
    val scale = math.max(screenW / w, screenH / h)
    Size(w * scale, h * scale)
  }

  /**
   * Tries to fill the whole screen, but will be smaller than the screen
   * and letterboxed if the aspect ratio does not match.
   */
  def fit(w: Float, h: Float) = {
    val scale = math.min(screenW / w, screenH / h)
    Size(w * scale, h * scale)
  }

  /**
   * Image stretched to fill the whole screen. Does not preserve aspect ratio.
   */
  def stretch(w: Float, h: Float) = {
    Size(screenW, screenH)
  }
}