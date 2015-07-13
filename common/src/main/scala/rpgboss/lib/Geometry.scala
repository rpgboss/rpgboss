package rpgboss.lib

import rpgboss.model.RpgEnum
import rpgboss.model.event.RawJs
import rpgboss.model.event.EventJavascript

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
  def tweenTo(other: Rect, alpha: Float) = {
    Rect(
        TweenUtils.tweenFloat(alpha, x, other.x),
        TweenUtils.tweenFloat(alpha, y, other.y),
        TweenUtils.tweenFloat(alpha, w, other.w),
        TweenUtils.tweenFloat(alpha, h, other.h))
  }
}

object SizeType extends RpgEnum {
  case class Val(i: Int, name: String, needParameters: Boolean)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val Fixed = Val(0, "Fixed", true)
  val ScaleSource = Val(1, "Scale_Source", true)
  val ProportionalToScreen = Val(2, "Proportional_To_Screen", true)
  val Cover = Val(3, "Cover_Screen", false)
  val Contain = Val(4, "Contain_In_Screen", false)

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

/**
 * The parameters have different units depending on the sizeTypeId.
 * If SizeType(sizeTypeId) is SizeType.Fixed, then they are in pixels.
 * Otherwise, they are a ratio with respect either the screen or the source
 * image.
 * @param   w   Ignored for Fill and Contain size types.
 * @param   h   Ignored for Fill and Contain size types.
 */
case class Layout(var layoutTypeId: Int = LayoutType.default.id,
                  var sizeTypeId: Int = SizeType.default.id,
                  var w: Float = 1.0f,
                  var h: Float = 1.0f,
                  var xOffset: Float = 0,
                  var yOffset: Float = 0) {

  def toJs(): RawJs = EventJavascript.jsCall(
      "game.layoutWithOffset", layoutTypeId, sizeTypeId, w, h, xOffset, yOffset)

  def getRect(srcW: Float, srcH: Float, screenW: Int, screenH: Int) = {
    import LayoutType._
    import SizeType._

    assume(screenW > 0)
    assume(screenH > 0)

    val (dstW, dstH, xOffsetPx, yOffsetPx): (Float, Float, Float, Float) =
      SizeType(sizeTypeId) match {
        case Fixed => (w, h, xOffset, yOffset)
        case ScaleSource => (srcW * w, srcH * h, srcW * xOffset, srcH * yOffset)
        case ProportionalToScreen =>
          (screenW * w, screenH * h, screenW * xOffset, screenH * yOffset)
        case Cover if srcW > 0 && srcH > 0 => {
          val scale = math.max(screenW / srcW, screenH / srcH)
          (srcW * scale, srcH * scale, srcW * xOffset, srcH * yOffset)
        }
        case Contain if srcW > 0 && srcH > 0 => {
          val scale = math.min(screenW / srcW, screenH / srcH)
          (srcW * scale, srcH * scale, srcW * xOffset, srcH * yOffset)
        }
        case _ => (0, 0, 0, 0)
      }

    val (x1, y1): (Float, Float) = LayoutType(layoutTypeId) match {
      case Centered => (screenW / 2, screenH / 2)
      case North => (screenW / 2, dstH / 2)
      case East => (dstW / 2, screenH / 2)
      case South => (screenW / 2, screenH - dstH / 2)
      case West => (screenW - dstW / 2, screenH / 2)
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
  def defaultForPictures =
    Layout(LayoutType.default.id, SizeType.ScaleSource.id, 1.0f, 1.0f)

  def empty = Layout(LayoutType.default.id, SizeType.default.id, 0, 0)
  def dummy = Layout(LayoutType.default.id, SizeType.default.id, 100, 100)
}