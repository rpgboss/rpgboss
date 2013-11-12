package rpgboss.model

object ItemScope extends RpgEnum {
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

case class CharProgressions(
  exp: Curve = Curve(300, 100), // Exp required to level up. Not cumulative.
  mhp: Curve = Curve(50, 10),
  mmp: Curve = Curve(20, 4),
  atk: Curve = Curve(10, 2),
  spd: Curve = Curve(10, 2),
  mag: Curve = Curve(10, 2))

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
  var startingEquipment: Seq[Int] = Seq(),
  var equipFixed: Seq[Int] = Seq()) extends HasName {
  def initMhp = progressions.mhp(initLevel)
  def initMmp = progressions.mmp(initLevel)
}

case class CharClass(
  var name: String = "",
  var canUseItems: Seq[Int] = Seq(),
  var effects: Seq[Effect] = Seq()) extends HasName

case class Enemy(
  var name: String = "",
  var battler: Option[BattlerSpec] = None,
  var level: Int = 5,
  var mhp: Int = 40,
  var mmp: Int = 40,
  var atk: Int = 10,
  var spd: Int = 10,
  var mag: Int = 10,
  var expValue: Int = 100,
  var effects: Seq[Effect] = Seq()) extends HasName
  
case class EncounterUnit(
  enemyIdx: Int,
  var x: Int,
  var y: Int)
  
case class Encounter(
  var name: String = "#<None>",
  var units: Seq[EncounterUnit] = Seq())

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

  equipType: Int = 0,
  
  accessId: Int = ItemAccessibility.default.id,
  scopeId: Int = ItemScope.default.id,

  icon: Option[IconSpec] = None) extends HasName
  
case class BattleStatsPermanent(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  statusEffects: Seq[StatusEffect])
  
object BattleStatsPermanent {
  def apply(allEquipment: Seq[Item], character: Character, level: Int, 
            equippedIds: Seq[Int]): BattleStatsPermanent = {
    require(equippedIds.forall(i => i >= 0 && i < allEquipment.length))

    val equipment = equippedIds.map(allEquipment)
    val allEffects = equipment.flatMap(_.effects)
    
    val mhp 
    
    apply(
      mhp = character.progressions.mhp(level),
      mmp = character.progressions.mmp(level),
      atk = character.progressions.atk(level),
      spd = character.progressions.spd(level),
      mag = character.progressions.mag(level)
      )
  }
}
  
  