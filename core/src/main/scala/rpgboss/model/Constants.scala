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

object MusicSlots {
  val BEGIN = 0
  val WEATHER = 7
  val NUM_SLOTS = 8
}

object PictureSlots {
  val BEGIN = 0

  val BELOW_MAP = 0
  val ABOVE_MAP = 8
  val MAP_END = 24

  val BATTLE_BEGIN = 24
  val BATTLE_BACKGROUND = 24
  val BATTLE_SPRITES_ENEMIES = 28
  val BATTLE_SPRITES_PARTY = 40
  val GAME_OVER = 49
  val BATTLE_END = 50

  val ABOVE_WINDOW = 50
  val WEATHER = 58

  val NUM_SLOTS = 64
}

object WeatherEffects extends RpgEnum {
  val RAIN = Value(1, "Rain")
  val FOG = Value(2, "Fog")

  def default = RAIN
}

object Transitions extends RpgEnum {

  val BaseBehaviour = Value(-1, "BaseBehaviour")
  val NONE = Value(0, "None")
  val FADE = Value(1, "Fade_Out")
  val Custom1 = Value(2, "Custom1")
  val Custom2 = Value(3, "Custom2")
  val Custom3 = Value(4, "Custom3")

  def default = FADE

  val fadeLength = 0.5f
}

object Origins extends RpgEnum {
  val SCREEN_TOP_LEFT = Value(0, "Top_Left")
  val SCREEN_CENTER = Value(1, "Screen_Center")
  val ON_ENTITY = Value(2, "On_Event_Player")

  def default = SCREEN_TOP_LEFT
}

object WeatherTypes extends RpgEnum {
  val NONE = Value(0, "None")
  val RAIN = Value(1, "Rain")
  val SNOW = Value(2, "Snow")

  def default = NONE
}

object Constants {

  val MINLEVEL = 1
  val MAXLEVEL = 9000

  val MINPRICE = 0
  val MAXPRICE = 999999

  val MINEFFECTARG = -9999
  val MAXEFFECTARG = 9999

  val NUM_VEHICLES = 4

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