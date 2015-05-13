package rpgboss.model

import Constants._

trait HasName {
  def name: String
}

object HasName {
  /**
   * Allows Strings to be used in functions that take HasName arguments.
   */
  implicit def StringToHasName(x: String) = new HasName {
    def name = x
  }
}

/**
 * These locations are given with the top-left of the map being at (0, 0).
 * This means that the center of the tiles are actually at 0.5 intervals.
 */
case class MapLoc(
  var map: String = "",
  var x: Float = 0f,
  var y: Float = 0f) {
  def isEmpty = map.isEmpty
}

object WhichEntity extends RpgEnum {
  val PLAYER = Value(0, "Player")
  val THIS_EVENT = Value(1, "This_Event")
  val EVENT_ON_MAP = Value(2, "Event_On_Map")
  val EVENT_ON_OTHER_MAP = Value(3, "Event_On_Other_Map")
  val NONE = Value(4, "None")

  def default = THIS_EVENT
}

object EntityInfoEnum extends RpgEnum {
  val X = Value(0, "X")
  val Y = Value(1, "Y")
  val FACE_DIRECTION = Value(2, "FACE_DIRECTION")
  val ScreenX = Value(3, "ScreenX")
  val ScreenY = Value(4, "ScreenY")
  val ScreenTopLeftX = Value(5, "ScreenTopLeftX")
  val ScreenTopLeftY = Value(6, "ScreenTopLeftY")
  val Width = Value(7, "Width")
  val Height = Value(8, "Height")

  def default = X
}

object EnabledDisabledEnum extends BooleanRpgEnum {
  val Disabled = Value(0, "Disabled")
  val Enabled = Value(1, "Enabled")
  def default = Disabled

  def fromBoolean(x: Boolean) = if (x) Enabled else Disabled
  def toBoolean(id: Int) = id == Enabled.id
}

object Scope extends RpgEnum {
  val None = Value
  val SelfOnly = Value("User_Only")
  val OneEnemy = Value("One_Enemy")
  val AllEnemies = Value("All_Enemies")
  val OneAlly = Value("One_Ally")
  val AllAllies = Value("All_Allies")
  val OneAllyDead = Value("One_Dead_Ally")
  val AllAlliesDead = Value("All_Dead_Allies")

  def default = OneAlly
}

object MetadataMode extends Enumeration {
  type MetadataMode = Value
  val Passability, Height = Value
}

object AddOrRemove extends BooleanRpgEnum {
  val Add, Remove = Value
  def default = Add

  def fromBoolean(x: Boolean) = if (x) Add else Remove
  def toBoolean(id: Int) = id == Add.id
}

object EquipOrUnequip extends BooleanRpgEnum {
  val Equip, Unequip = Value
  def default = Equip

  def fromBoolean(x: Boolean) = if (x) Equip else Unequip
  def toBoolean(id: Int) = id == Equip.id
}

object HealOrDamageEnum extends BooleanRpgEnum {
  val Heal, Damage = Value
  def default = Heal

  def fromBoolean(x: Boolean) = if (x) Heal else Damage
  def toBoolean(id: Int) = id == Heal.id
}

case class BattlerSpec(var name: String, var scale: Float = 1.0f)

// Specifies an entity: Either the player or an event on the current map.
case class EntitySpec(
  var whichEntityId: Int = WhichEntity.default.id,
  var mapName: String = "",
  var eventId: Int = -1)

object EntitySpec {
  def playerEntityId = -1
  /**
   * This is a total hack but it does seem to make things simple.
   */
  def vehicleEntityId(vehicleId: Int) = -2 - vehicleId
}

case class IconSpec(iconset: String, iconX: Int, iconY: Int)

case class RandomEncounter(var encounterId: Int = 0, var weight: Int = 10)

case class RandomEncounterSettings(
  var stepsAverage: Int = 20,
  var encounters: Array[RandomEncounter] = Array()) {
  def deepcopy() = copy(encounters = encounters.map(_.copy()))
}

case class ColorSpec(
  var r: Float = 1.0f,
  var g: Float = 0.0f,
  var b: Float = 0.0f,
  var a: Float = 0.5f)

case class FaceSpec(
  var faceset: String,
  var faceX: Int,
  var faceY: Int)

case class SoundSpec(
  sound: String = "",
  var volume: Float = 1.0f,
  var pitch: Float = 1.0f)

case class SpriteSpec(
  name: String,
  spriteIndex: Int,
  dir: Int = SpriteSpec.Directions.SOUTH,
  step: Int = SpriteSpec.Steps.STILL)

object SpriteSpec {
  object Directions {
    val SOUTH = 0
    val WEST = 1
    val EAST = 2
    val NORTH = 3
    val NONE = -1

    def opposite(dir: Int) = dir match {
      case SOUTH => NORTH
      case WEST => EAST
      case EAST => WEST
      case NORTH => SOUTH
      case _ => NONE
    }
  }

  object Steps {
    val STEP0 = 0
    val STEP1 = 1
    val STEP2 = 2
    val STEP3 = 3

    val STILL = 1

    val TOTALSTEPS = 4
  }
}
