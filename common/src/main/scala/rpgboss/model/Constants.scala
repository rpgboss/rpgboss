package rpgboss.model

trait RpgEnum extends Enumeration {
  def get(v: Int) = apply(v)
  
  def findOrDefault(s: String) = {
    values.find(_.toString == s).getOrElse(default)
  }
  def default: Value
}

object Constants {

  val MINLEVEL = 1
  val MAXLEVEL = 9000

  val MINPRICE = 0
  val MAXPRICE = 999999

  val MINEFFECTARG = -9999
  val MAXEFFECTARG = 9999

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
    val BACKGROUND = 0
    val FOREGROUND = 8
    val OVER_MAP_BACKGROUND = 24
    val OVER_MAP_FOREGOUND = 32
    val ABOVE_ALL = 56
    val TOTAL = 64
  }

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

  object Transitions extends RpgEnum {
    val NONE = Value(0, "None")
    val FADE = Value(1, "Fade out")

    def default = FADE

    val fadeLength = 500
  }

  object Scope extends RpgEnum {
    val None = Value
    val SelfOnly = Value("User only")
    val OneEnemy = Value("One enemy")
    val AllEnemies = Value("All enemies")
    val OneAlly = Value("One ally")
    val AllAllies = Value("All allies")
    val OneAllyDead = Value("One dead ally")
    val AllAlliesDead = Value("All dead allies")

    def default = OneAlly
  }

  object ItemType extends RpgEnum {
    val Consumable, Reusable, Equipment = Value
    val KeyItem = Value("Key item")

    def default = Consumable
  }

  object ItemAccessibility extends RpgEnum {
    val Always = Value
    val MenuOnly = Value("Menu only")
    val BattleOnly = Value("Battle only")
    val Never = Value

    def default = Always
  }

  object EquipSlot extends RpgEnum {
    val None, Weapon, Offhand, Armor, Head, Accessory = Value
    def default = None
  }
}