package rpgboss.model

import rpgboss._
import rpgboss.model.battle._

class SkillSpec extends UnitSpec {
  def fixture = new {
    val pData = ProjectData()

    val status = new BattleStatus(
      0,
      pData,
      BattleEntityType.Party,
      0,
      50,
      20,
      BaseStats(50, 20, 10, 10, 10, arm = 100, mre = 200, effects = Array()),
      equipment = Array(),
      onAttackSkillIds = Array(),
      knownSkillIds = Array(),
      initialTempStatusEffectIds = Array(),
      row = 0)
  }

  "Skill" should "use RecoverHpAdd effects" in {
    val f = fixture
    f.status.hp = 20

    val skill = Skill(damages = Array(),
                      effects = Array(Effect(RecoverHpAdd.id, 20, 0)))
    val hits = skill.applySkill(f.status, f.status)
    hits.length should equal (1)
    hits.head.damage.value should equal (-20)

    f.status.hp should equal (40)
  }

  "Skill" should "use RecoverHpAddMul effects" in {
    val f = fixture
    f.status.hp = 1

    val skill = Skill(damages = Array(),
                      effects = Array(Effect(RecoverHpMul.id, 10, 0)))
    val hits = skill.applySkill(f.status, f.status)
    hits.length should equal (1)
    hits.head.damage.value should equal (-5)

    f.status.hp should equal (6)
  }

  "Skill" should "use RecoverMpAdd effects" in {
    val f = fixture
    f.status.mp = 1

    val skill = Skill(damages = Array(),
                      effects = Array(Effect(RecoverMpAdd.id, 10, 0)))
    val hits = skill.applySkill(f.status, f.status)
    hits.length should equal (1)
    hits.head.damage.value should equal (-10)

    f.status.mp should equal (11)
  }

  "Skill" should "use RecoverMpAddMul effects" in {
    val f = fixture
    f.status.mp = 1

    val skill = Skill(damages = Array(),
                      effects = Array(Effect(RecoverMpMul.id, 20, 0)))
    val hits = skill.applySkill(f.status, f.status)
    hits.length should equal (1)
    hits.head.damage.value should equal (-4)

    f.status.mp should equal (5)
  }

  "Skill" should "apply and remove status effects" in {
    val f = fixture
    f.pData.enums.statusEffects = Array.fill(4)(StatusEffect())

    // Use a skill to add a status effect.
    val addSkill = Skill(
        damages = Array(), effects = Array(Effect(AddStatusEffect.id, 3, 100)))
    val addHits = addSkill.applySkill(f.status, f.status)
    addHits.length should equal (1)
    addHits.head.damage.value should equal (3)

    f.status.tempStatusEffectIds should deepEqual(Array(3))

    // Add another stack
    addSkill.applySkill(f.status, f.status)
    f.status.tempStatusEffectIds should deepEqual(Array(3, 3))

    // Use a skill to remove a status effect.
    val removeSkill = Skill(
        damages = Array(),
        effects = Array(Effect(RemoveStatusEffect.id, 3, 100)))
    val removeHits = removeSkill.applySkill(f.status, f.status)
    removeHits.length should equal (1)
    removeHits.head.damage.value should equal (3)

    f.status.tempStatusEffectIds should deepEqual(Array[Int]())

    // Add two stacks of the status effect
    val addDifferentStatusEffect = Skill(
        damages = Array(), effects = Array(Effect(AddStatusEffect.id, 1, 100)))

    addDifferentStatusEffect.applySkill(f.status, f.status)
    addSkill.applySkill(f.status, f.status)
    addSkill.applySkill(f.status, f.status)
    f.status.tempStatusEffectIds should deepEqual(Array(1, 3, 3))

    // Remove all status effects.
    val removeAllStatusEffect = Skill(
        damages = Array(),
        effects = Array(Effect(RemoveAllStatusEffect.id, 0, 100)))
    removeAllStatusEffect.applySkill(f.status, f.status)

    f.status.tempStatusEffectIds should deepEqual(Array[Int]())
  }
}