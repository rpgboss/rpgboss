package rpgboss.model

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
      CharState("Dead", 
          Map(NoAction->1)),
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
  def defaultItemTypes = Array(
    "Consumable",
    "Quest Item",
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
  
  def defaultItems = Array(
      Item("Potion",        "Restores 30% of HP", 0, Map("HpResMul"->30)),
      Item("Big Potion",    "Restores 60% of HP", 0, Map("HpResMul"->60)),
      Item("Master Potion", "Restores 100% of HP", 0, Map("HpResMul"->100)),
      )
  )
}
    
case class Item(
    name: String,
    desc: String,
    
    itemType: Int,
    price: Int = -1,
    
    effects: Map[String, Int],
    
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
