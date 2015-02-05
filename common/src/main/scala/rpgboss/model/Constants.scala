package rpgboss.model

trait RpgEnum extends Enumeration {
  def get(v: Int) = apply(v)

  def findOrDefault(s: String) = {
    values.find(_.toString == s).getOrElse(default)
  }
  def default: Value
}

trait BooleanRpgEnum extends RpgEnum {
  def fromBoolean(x: Boolean): Value
  def toBoolean(id: Int): Boolean
}

object DirectionMasks {
  val NORTH = 1 << 0
  val EAST = 1 << 1
  val SOUTH = 1 << 2
  val WEST = 1 << 3
  val NE = 1 << 4
  val SE = 1 << 5
  val SW = 1 << 6
  val NW = 1 << 7

  val ALLCARDINAL = NORTH | EAST | SOUTH | WEST
  val NONE = 0

  def allBlocked(b: Byte) = (b & ALLCARDINAL) == ALLCARDINAL
  def flagged(b: Byte, dir: Int) = (b & dir) == dir
}

object PictureSlots {
  val BEGIN = 0
  val BELOW_MAP = 0
  val ABOVE_MAP = 8
  val BATTLE_BEGIN = 24
  val BATTLE_BACKGROUND = 24
  val BATTLE_SPRITES_ENEMIES = 28
  val BATTLE_SPRITES_PARTY = 40
  val BATTLE_END = 50
  val ABOVE_WINDOW = 50
  val END = 64
}

object Transitions extends RpgEnum {
  val NONE = Value(0, "None")
  val FADE = Value(1, "Fade_Out")

  def default = FADE

  val fadeLength = 0.5f
}

object Constants {

  val MINLEVEL = 1
  val MAXLEVEL = 9000

  val MINPRICE = 0
  val MAXPRICE = 999999

  val MINEFFECTARG = -9999
  val MAXEFFECTARG = 9999

  import DirectionMasks._

  val DirectionOffsets = Map(
    NORTH -> (0, -1),
    EAST -> (1, 0),
    SOUTH -> (0, 1),
    WEST -> (-1, 0),
    NE -> (1, -1),
    SE -> (1, 1),
    SW -> (-1, 1),
    NW -> (-1, -1))
}