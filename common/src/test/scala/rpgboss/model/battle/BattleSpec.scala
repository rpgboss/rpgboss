package rpgboss.model.battle

import rpgboss._
import rpgboss.model._
import rpgboss.model.battle._

object BattleTest {
  class BattleFixture(aiOpt: Option[BattleAI] = None) {
    val pData = ProjectData("fake-uuid", "fake-title")

    val characterFast =
      Character(progressions = StatProgressions(spd = Curve(10, 2)))
    val characterSlow =
      Character(progressions = StatProgressions(spd = Curve(4, 2)))
    val enemyMedium =
      Enemy(spd = 8)

    pData.enums.characters = Array(characterFast, characterSlow)
    pData.enums.enemies = Array(enemyMedium)

    def encounter =
      Encounter(units = Array(EncounterUnit(0, 100, 100)))

    def initialCharacterHps = Array(1, 1)

    val battle = new Battle(
      pData = pData,
      partyIds = Array(0, 1),
      PartyParameters(
        characterLevels = Array(1, 1),
        initialCharacterHps = initialCharacterHps,
        initialCharacterMps = Array(1, 1),
        characterEquip = Array(Array(), Array()),
        initialCharacterTempStatusEffectIds = Array(Array(), Array()),
        learnedSkills = Array(Array(), Array()),
        characterRows = Array(0, 0)),
      encounter = encounter,
      aiOpt = aiOpt)
  }
}

class BattleSpec extends UnitSpec {
  "Battle" should "make fastest unit go first" in {
    val f = new BattleTest.BattleFixture

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.entityId should equal (0)

    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))

    f.battle.readyEntity should equal (None)
  }

  "Battle" should "have battle units act in order of speed" in {
    val f = new BattleTest.BattleFixture

    f.battle.advanceTime(f.battle.baseTurnTime)

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.entityId should equal (0)
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Enemy)
    f.battle.readyEntity.get.entityId should equal (0)
    f.battle.takeAction(NullAction(f.battle.enemyStatus(0)))

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.entityId should equal (1)
    f.battle.takeAction(NullAction(f.battle.partyStatus(1)))

    f.battle.readyEntity should equal (None)
  }

  "Battle" should "use AI to automatically handle enemy actions" in {
    val f = new BattleTest.BattleFixture(aiOpt = Some(new RandomEnemyAI))

    f.battle.advanceTime(f.battle.baseTurnTime)

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.entityId should equal (0)
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))

    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.entityId should equal (1)
    f.battle.takeAction(NullAction(f.battle.partyStatus(1)))

    f.battle.readyEntity should equal (None)
  }

  "Battle" should "reassign attacks on dead party members to live ones" in {
    val f = new BattleTest.BattleFixture(aiOpt = Some(new RandomEnemyAI)) {
      override def initialCharacterHps = Array(0, 1)
    }

    val action =
      AttackAction(f.battle.enemyStatus.head, Array(f.battle.partyStatus.head))
    val (hits, success) = action.process(f.battle)

    hits.length should equal (1)
    hits.head.hitActor should equal (f.battle.partyStatus(1))
  }

  "Battle" should "reassign attacks on dead enemies to live ones" in {
    val f = new BattleTest.BattleFixture {
      override def encounter = Encounter(
        units = Array(EncounterUnit(0, 100, 100), EncounterUnit(0, 100, 100)))
    }

    f.battle.enemyStatus.head.hp = 0
    f.battle.enemyStatus.head.alive should equal (false)

    val action =
      AttackAction(f.battle.partyStatus.head, Array(f.battle.enemyStatus.head))
    val (hits, success) = action.process(f.battle)

    hits.length should equal (1)
    hits.head.hitActor should equal (f.battle.enemyStatus(1))
  }

  "Battle" should "heal targets up to, but not exceeding, their max HP" in {
    val f = new BattleTest.BattleFixture

    f.pData.enums.skills =
      Array(
        Skill(damages = Array(
          DamageFormula(typeId = DamageType.Magic.id, elementId = 0,
                 formula = "-a.mag*10")))
    )

    val partyHead = f.battle.partyStatus.head

    val action = SkillAction(partyHead, Array(partyHead), skillId = 0)
    val (hits, success) = action.process(f.battle)

    hits.length should equal (1)
    hits.head.hitActor should equal (partyHead)

    // The reported healing value should exceed the HP
    hits.head.damage should equal (Damage(DamageType.Magic, 0, -91))

    // But the final HP should equal the max HP
    partyHead.hp should equal(partyHead.stats.mhp)
  }

  "Battle" should "handle victory correctly" in {
    val f = new BattleTest.BattleFixture
    f.battle.enemyStatus.map(_.hp = 0)
    f.battle.advanceTime(0f)
    f.battle.state should equal(Battle.VICTORY)
  }

  "Battle" should "handle defeat correctly" in {
    val f = new BattleTest.BattleFixture
    f.battle.partyStatus.map(_.hp = 0)
    f.battle.advanceTime(0f)
    f.battle.state should equal(Battle.DEFEAT)
  }
}