package rpgboss.model

import Constants._
import Curve.Linear

trait HasName {
  def name: String
  override def toString = name
}

/**
 * These locations are given with the top-left of the map being at (0, 0).
 * This means that the center of the tiles are actually at 0.5 intervals.
 */
case class MapLoc(
  map: String,
  var x: Float,
  var y: Float)

object WhichEntity extends RpgEnum {
  val PLAYER = Value(0, "Player")
  val THIS_EVENT = Value(1, "This event")
  val OTHER_EVENT = Value(2, "Other event")
  
  def default = PLAYER
}
  
// Specifies an entity: Either the player or an event on the current map.
case class EntitySpec(
  var whichEntityId: Int = WhichEntity.default.id,
  var eventId: Int = -1)

case class SpriteSpec(
  name: String,
  spriteIndex: Int,
  dir: Int = SpriteSpec.Directions.SOUTH,
  step: Int = SpriteSpec.Steps.STILL)
  
case class BattlerSpec(name: String)
  
case class SoundSpec(
  sound: String,
  volume: Float = 1.0f,
  pitch: Float = 1.0f)

case class IconSpec(iconset: String, iconX: Int, iconY: Int)

case class Curve(a: Int, b: Int, c: Int) {
  def apply(x: Int) = {
    a * x * x + b * x + c
  }
}

object Curve {
  def Linear(slope: Int, intercept: Int) =
    Curve(0, slope, intercept)
}
case class CharProgressions(
  exp: Curve = Curve(10, 10, 0),
  mhp: Curve = Linear(25, 50),
  mmp: Curve = Linear(5, 20),
  str: Curve = Linear(3, 10),
  dex: Curve = Linear(3, 10),
  con: Curve = Linear(3, 10),
  int: Curve = Linear(3, 10),
  wis: Curve = Linear(3, 10),
  cha: Curve = Linear(3, 10))

case class EquipSet(
  weapon: Int,
  offhand: Int,
  armor: Int,
  helmet: Int,
  acc1: Int,
  acc2: Int)

case class EquipSetBool(
  weapon: Boolean = false,
  offhand: Boolean = false,
  armor: Boolean = false,
  helmet: Boolean = false,
  acc1: Boolean = false,
  acc2: Boolean = false)

object EquipSet {
  def empty = EquipSet(-1, -1, -1, -1, -1, -1)
}

/**
 * @param startingEquipment   Denotes the item ids of starting equipment.
 *                            A value of -1 means it's an empty slot.
 *
 * @param equipFixed          "true" means the player cannot modify this slot.
 */
case class Character(
  var name: String = "",
  var subtitle: String = "",
  var description: String = "",
  var sprite: Option[SpriteSpec] = None,
  var initLevel: Int = 1, var maxLevel: Int = 50,
  var charClass: Int = 0,
  var progressions: CharProgressions = CharProgressions(),
  var startingEquipment: EquipSet = EquipSet.empty,
  var equipFixed: EquipSetBool = EquipSetBool()) extends HasName {
  def initMhp = progressions.mhp(initLevel)
  def initMmp = progressions.mmp(initLevel)
}

case class CharClass(
  var name: String = "",
  var canUseEquipSubtypes: Seq[Int] = Seq(),
  var effects: Seq[Effect] = Seq()) extends HasName

case class Enemy(
  var name: String = "",
  var battler: Option[BattlerSpec] = None,
  var level: Int = 5,
  var mhp: Int = 40,
  var mmp: Int = 40,
  var str: Int = 10,
  var dex: Int = 10,
  var con: Int = 10,
  var int: Int = 10,
  var wis: Int = 10,
  var cha: Int = 10,
  var expValue: Int = 100,
  var effects: Seq[Effect] = Seq()) extends HasName

case class Skill(name: String = "") extends HasName

object CharState {

  /*
   * val defaultStates = 
      CharState("Dead",      Map("NoAction"->1)),
      CharState("Stunned",   Map("NoAction"->1),     1, 100),
      CharState("Berserk",   Map("AutoAtkEnemy"->1,
                                 "AtkMul"-> 75),     8, 25),
      CharState("Poisoned",  Map("HpRegenMul"-> -5), 8, 50,  0,  3),
      CharState("Mute",      Map("NoMagic"->1),      2, 100),
      CharState("Confused",  Map("AutoAtkAlly"->1),  3, 50,  50),
      CharState("Asleep",    Map("NoAction"->1),     6, 50,  100),
      CharState("Paralyzed", Map("NoAction"->1),     3, 25,  25),
      CharState("Blinded",   Map("DexMul"-> -50),    8, 50),
      CharState("Weakened",  Map("AtkMul"-> -30),    4, 100, 0,  3),
      CharState("Hasted",    Map("DexMul"-> 50),     4, 100, 0,  2),
      CharState("Slowed",    Map("DexMul"-> -50),    4, 100, 0,  2)
  )*/

}

case class Effect(key: String, v: Int)

case class StatusEffect(
  name: String = "",
  effects: Seq[Effect] = Seq(),
  releaseOnBattleEnd: Boolean = false,
  releaseTime: Int = 0,
  releaseChance: Int = 0,
  releaseDmgChance: Int = 0,
  maxStacks: Int = 1) extends HasName

object Item {
}

case class Item(
  name: String = "",
  desc: String = "",
  effects: Seq[Effect] = Seq(),

  sellable: Boolean = true,
  price: Int = 100,

  itemType: Int = ItemType.default.id,

  slot: Int = EquipSlot.default.id,
  equipSubtype: Int = 0,

  accessId: Int = ItemAccessibility.default.id,
  scopeId: Int = Scope.default.id,

  icon: Option[IconSpec] = None) extends HasName

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
