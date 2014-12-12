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
case class Rect(x: Float, y: Float, w: Float, h: Float) extends BoxLike

object SizeType extends RpgEnum {
  case class Val(i: Int, name: String, needParameters: Boolean)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val Fixed = Val(0, "Fixed", true)
  val ScaleSource = Val(1, "Scale source", true)
  val ProportionalToScreen = Val(2, "Proportional to Screen", true)
  val Cover = Val(3, "Cover screen", false)
  val Contain = Val(4, "Contain in screen", false)

  def default = Fixed
}

object LayoutType extends RpgEnum {
  case class Val(i: Int, name: String, functionName: String)
    extends super.Val(i, name)

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

case class Layout(layoutTypeId: Int, sizeTypeId: Int,
                  wPx: Int, hPx: Int,
                  wRatio: Float, hRatio: Float,
                  xOffsetPx: Int, yOffsetPx: Int) {
  def getRect(srcW: Int, srcH: Int, screenW: Int, screenH: Int) = {
    import LayoutType._
    import SizeType._

    assume(srcW > 0)
    assume(srcH > 0)
    assume(screenW > 0)
    assume(screenH > 0)

    val (dstW, dstH): (Float, Float) = SizeType(sizeTypeId) match {
      case Fixed => (wPx, hPx)
      case ScaleSource => (srcW * wRatio, srcH * hRatio)
      case ProportionalToScreen => (screenW * wRatio, screenH * hRatio)
      case Cover if srcW > 0 && srcH > 0 => {
        val scale = math.max(screenW / srcW, screenH / srcH)
        (srcW * scale, srcH * scale)
      }
      case Contain if srcW > 0 && srcH > 0 => {
        val scale = math.min(screenW / srcW, screenH / srcH)
        (srcW * scale, srcH * scale)
      }
      case _ => (0, 0)
    }

    val (x1, y1): (Float, Float) = LayoutType(layoutTypeId) match {
      case Centered => (screenW / 2, screenH / 2)
      case North => (screenW / 2, dstH / 2)
      case East => (dstW / 2, screenH / 2)
      case South => (screenW / 2, screenH - dstH / 2)
      case West => (screenW - dstW /2, screenH / 2)
      case NorthEast => (screenW - dstW / 2, dstH / 2)
      case SouthEast => (screenW - dstW / 2, screenH - dstH / 2)
      case SouthWest => (dstW / 2, screenH - dstH / 2)
      case NorthWest => (dstW / 2, dstH / 2)
      case _ => (0, 0)
    }

    Rect(x1 + xOffsetPx, y1 + yOffsetPx, dstW, dstH)
  }
}

object Layout {
  // Convenience constructor
  def apply(layoutTypeId: Int, sizeTypeId: Int, wArg: Float, hArg: Float,
            xOffset: Int = 0, yOffset: Int = 0): Layout = {
    import SizeType._
    val (wPx, yPx, wRatio, yRatio): (Int, Int, Float, Float) =
      SizeType(sizeTypeId) match {
        case Fixed => (wArg.round, hArg.round, 0, 0)
        case _ => (0, 0, wArg, hArg)
      }
    Layout(layoutTypeId, sizeTypeId, wPx, yPx, wRatio, yRatio, xOffset, yOffset)
  }
}