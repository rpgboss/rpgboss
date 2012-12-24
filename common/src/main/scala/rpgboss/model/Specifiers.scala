package rpgboss.model

import Constants._

/**
 * These locations are given with the top-left of the map being at (0, 0).
 * This means that the center of the tiles are actually at 0.5 intervals. 
 */
case class MapLoc(
    map: String, 
    var x: Float, 
    var y: Float)
    
case class EvtPath(mapName: String, evtName: String)

case class SpriteSpec(
    spriteset: String, 
    spriteIndex: Int,
    dir: Int = SpriteSpec.Directions.SOUTH,
    step: Int = SpriteSpec.Steps.STILL)

case class IconSpec(iconset: String, iconX: Int, iconY: Int)

case class Curve(a: Int, b: Int, c: Int) {
  def apply(x: Int) = {
    a*x*x + b*x + c
  }
}

object Curve {
  def Linear(slope: Int, intercept: Int) =
    Curve(0, slope, intercept)
}

import Curve.Linear
case class CharProgressions(
    exp: Curve = Curve(10, 10, 0),
    hp:  Curve = Linear(25, 50),
    sp:  Curve = Linear(5, 20),
    str: Curve = Linear(3, 10),
    dex: Curve = Linear(3, 10),
    con: Curve = Linear(3, 10),
    int: Curve = Linear(3, 10),
    wis: Curve = Linear(3, 10),
    cha: Curve = Linear(3, 10)
)

case class EquipSet(
    weapon: Int,
    offhand: Int,
    armor: Int,
    helmet: Int,
    acc1: Int,
    acc2: Int
)

object EquipSet {
  def empty = EquipSet(-1, -1, -1, -1, -1, -1)
  def zeroes = EquipSet(0, 0, 0, 0, 0, 0)
}

/**
 * @param startingEquipment   Denotes the item ids of starting equipment.
 *                            A value of -1 means it's an empty slot.
 *                            
 * @param equipFixed          A value of 0 in this slot means the player can
 *                            modify what's in this slot. Any other value means 
 *                            the means the player cannot.
 */
case class Character(
    defaultName: String = "", 
    subtitle:    String = "",
    description: String = "",
    sprite: Option[SpriteSpec] = None,
    initLevel: Int = 1, maxLevel: Int = 50,
    progressions: CharProgressions = CharProgressions(),
    startingEquipment: EquipSet = EquipSet.empty,
    equipFixed: EquipSet = EquipSet.zeroes)

case class Skill()

object CharState {
  val defaultStates = Array()
  /*
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

case class CharState(
    name: String, 
    effects: Map[String, Int],
    releaseTime: Int = -1,
    releaseChance: Int = 0,
    releaseDmgChance: Int = 0,
    maxStacks: Int = 1)

object Item {
}
    
case class Item(
    name: String = "",
    desc: String = "",
    effects: Map[String, Int] = Map(),
    price: Int = -1,
    
    itemType: Int = ItemType.Consumable.id,
    
    slot: Int = EquipSlot.default.id,
    scopeId: Int = Scope.default.id,
    icon: Option[IconSpec] = None)

object SpriteSpec {
  object Directions {
    val SOUTH = 0
    val WEST  = 1
    val EAST  = 2
    val NORTH = 3
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
