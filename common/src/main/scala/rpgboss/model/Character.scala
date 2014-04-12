package rpgboss.model

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
  var progressions: StatProgressions = StatProgressions(),
  var startingEquipment: Seq[Int] = Seq(),
  var equipFixed: Seq[Int] = Seq()) extends HasName {
  def baseStats(pData: ProjectData, level: Int) = {
    val effects = {
      if (charClass >= 0 && charClass < pData.enums.classes.length)
        pData.enums.classes(charClass).effects
      else
        Seq()
    }

    BaseStats(
      mhp = progressions.mhp(level),
      mmp = progressions.mmp(level),
      atk = progressions.atk(level),
      spd = progressions.spd(level),
      mag = progressions.mag(level),
      arm = progressions.arm(level),
      mre = progressions.mre(level),
      effects = effects
    )
  }
}

case class LearnedSkill(var level: Int, var skillId: Int)

case class CharClass(
  var name: String = "",
  var canUseItems: Seq[Int] = Seq(),
  var effects: Seq[Effect] = Seq(),
  var learnedSkills: Seq[LearnedSkill] = Seq()) extends HasName

case class Enemy(
  var name: String = "",
  var battler: Option[BattlerSpec] = None,
  var level: Int = 5,
  var mhp: Int = 40,
  var mmp: Int = 40,
  var atk: Int = 10,
  var spd: Int = 10,
  var mag: Int = 10,
  var arm: Int = 10,
  var mre: Int = 10,
  var expValue: Int = 100,
  var effects: Seq[Effect] = Seq(),
  var skills: Seq[Int] = Seq()) extends HasName {
  def baseStats =
    BaseStats(
      mhp = mhp,
      mmp = mmp,
      atk = atk,
      spd = spd,
      mag = mag,
      arm = arm,
      mre = mre,
      effects = effects)
}

case class EncounterUnit(
  enemyIdx: Int,
  var x: Int,
  var y: Int)

case class Encounter(
  var name: String = "",
  var units: Seq[EncounterUnit] = Seq()) extends HasName

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
