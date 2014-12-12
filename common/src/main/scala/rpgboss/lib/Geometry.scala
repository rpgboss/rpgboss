package rpgboss.lib

import rpgboss.model.RpgEnum

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
  def right = x + w / 2
  def top = y - h / 2
  def bot = y + h / 2

  def getRect() = Rect(x, y, w, h)
}

/**
 * @param   x   Refers to center of the rectangle.
 * @param   y   Refers to center of the rectangle.
 */
case class Rect(x: Float, y: Float, w: Float, h: Float) extends BoxLike {
  def offset(xOffset: Float, yOffset: Float) =
    copy(x = x + xOffset, y = y + yOffset)
}

case class Size(w: Float, h: Float)

/**
 * @param   screenW   Should always be an integer, but treated as a float for
 *                    convenience.
 * @param   screenH   Should always be an integer, but treated as a float for
 *                    convenience.
 */
class LayoutProvider(screenW: Float, screenH: Float) {
  def north(w: Float, h: Float) =
    Rect(screenW / 2, h / 2, w, h)
  def north(s: Size): Rect = north(s.w, s.h)

  def south(w: Float, h: Float) =
    Rect(screenW / 2, screenH - h / 2, w, h)
  def south(s: Size): Rect = south(s.w, s.h)

  def east(w: Float, h: Float) =
    Rect(w / 2, screenH / 2, w, h)
  def east(s: Size): Rect = east(s.w, s.h)

  def west(w: Float, h: Float) =
    Rect(screenW - w /2, screenH / 2, w, h)
  def west(s: Size): Rect = west(s.w, s.h)

  def northeast(w: Float, h: Float) =
    Rect(screenW - w / 2, h / 2, w, h)
  def northeast(s: Size): Rect = northeast(s.w, s.h)

  def northwest(w: Float, h: Float) =
    Rect(w / 2, h / 2, w, h)
  def northwest(s: Size): Rect = northwest(s.w, s.h)

  def southeast(w: Float, h: Float) =
    Rect(screenW - w / 2, screenH - h / 2, w, h)
  def southeast(s: Size): Rect = southeast(s.w, s.h)

  def southwest(w: Float, h: Float) =
    Rect(w / 2, screenH - h / 2, w, h)
  def southwest(s: Size): Rect = southwest(s.w, s.h)

  def centered(w: Float, h: Float) =
    Rect(screenW / 2, screenH / 2, w, h)
  def centered(s: Size): Rect = centered(s.w, s.h)
}

object SizeType extends RpgEnum {
  case class Val(i: Int, name: String, needParameters: Boolean)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val Fixed = Val(0, "Fixed", true)
  val Proportional = Val(1, "Proportional", true)
  val Cover = Val(2, "Cover", false)
  val Contain = Val(3, "Contain", false)

  def default = Fixed
}

object LayoutType extends RpgEnum {
  val Centered = Value(0)
  val North = Value(1)
  val East = Value(2)
  val South = Value(3)
  val West = Value(4)
  val NorthEast = Value(5)
  val SouthEast = Value(6)
  val SouthWest = Value(7)
  val NorthWest = Value(8)

  def default = Centered
}

class SizeProvider(screenW: Float, screenH: Float) {
  def fixed(w: Float, h: Float) = Size(w, h)

  def prop(xProportion: Float, yProportion: Float) =
    Size(xProportion * screenW, yProportion * screenH)

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