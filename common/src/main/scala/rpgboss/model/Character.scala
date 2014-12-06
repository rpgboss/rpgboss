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
  var startingEquipment: Array[Int] = Array(),
  var equipFixed: Array[Int] = Array()) extends HasName {

  def expToLevel(level: Int) = progressions.exp(level)

  def baseStats(pData: ProjectData, level: Int) = {
    val effects: Array[Effect] = {
      if (charClass >= 0 && charClass < pData.enums.classes.length)
        pData.enums.classes(charClass).effects
      else
        Array()
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

  var unarmedAttackSkillId: Int = 0,

  var canUseItems: Array[Int] = Array(),
  var effects: Array[Effect] = Array(),
  var learnedSkills: Array[LearnedSkill] = Array()) extends HasName {
  def knownSkillIds(level: Int): Array[Int] = {
    learnedSkills.filter(_.level  <= level).map(_.skillId)
  }
}
