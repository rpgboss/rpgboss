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

case class Character(defaultName: String, sprite: SpriteSpec)

object CharState {
  val defaultStates = Array(
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
  )
  
}

case class CharState(
    name: String, 
    effects: Map[String, Int],
    releaseTime: Int = -1,
    releaseChance: Int = 0,
    releaseDmgChance: Int = 0,
    maxStacks: Int = 1)

object Item {
  def defaultEquipTypes = Array(
    "Weapon-Light",
    "Weapon-Medium",
    "Weapon-Heavy",
    "Weapon-Ranged",
    "Shield",
    "Offhand-Misc",
    "Armor-Light",  
    "Armor-Medium",
    "Armor-Heavy",
    "Helm",
    "Accessory"
  )
  
  def defaultItems: Array[Item] = Array(
      Item("Potion", "Restores 30% of HP", Map("HpResMul"->30), 50),
      Item("Big Potion", "Restores 60% of HP", Map("HpResMul"->60), 150),
      Item("Full Potion", "Restores 100% of HP", Map("HpResMul"->100), 500),
      Item("Magic Dust", "Restore 30% of MP", Map("MpResMul"->30), 100), 
      Item("Magic Crystal", "Restore 60% of MP", Map("MpResMul"->60), 300),
      Item("Elixir", "Restore 30% of HP and MP",
          Map("HpResMul"->30, "MpResMul"->30), 500),
      Item("Mega Elixir", "Restore 60% of HP and MP",
          Map("HpResMul"->60, "MpResMul"->60), 1500),
      Item("Dispel Orb", "Dispels all status effects",
          Map("Dispel_All"->1), 100),
      Item("Unicorn blood", "Revives to life with 30% HP",
          Map("Dispel_0"->1, "HpResMul"->30), 500),
      Item("Phoenix feather", "Revives to life with 60% HP",
          Map("Dispel_0"->1, "HpResMul"->30), 1500),
      
      Item("Magic flute", "Wakes party up", 
          Map("Dispel_6"->1), itemType = ItemType.Rare.id)
      
  )
}
    
case class Item(
    name: String,
    desc: String,
    
    effects: Map[String, Int],
    
    itemType: Int = ItemType.Consumable.id,
    
    equipType: Int = 0,
    
    price: Int = -1,
    scopeId: Int = ItemScope.default.id,
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
