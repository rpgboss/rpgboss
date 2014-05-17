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
    
    val battle = new Battle(
      pData = pData, 
      partyIds = Array(0, 1), 
      characterLevels = Array(1, 1),
      initialCharacterHps = Array(1, 1),
      initialCharacterMps = Array(1, 1),
      characterEquip = Array(Array(), Array()),
      initialCharacterTempStatusEffects = Array(Array(), Array()),
      characterRows = Array(0, 0),
      encounter = Encounter(units = Array(EncounterUnit(0, 100, 100))),
      aiOpt = aiOpt)
  }
}

class BattleSpec extends UnitSpec {
  "Battle" should "make fastest unit go first" in {
    val f = new BattleTest.BattleFixture
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    
    f.battle.readyEntity should equal (None)
  }
  
  "Battle" should "have battle units act in order of speed" in {
    val f = new BattleTest.BattleFixture
    
    f.battle.advanceTime(f.battle.baseTurnTime)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Enemy)
    f.battle.readyEntity.get.id should equal (0)
    f.battle.takeAction(NullAction(f.battle.enemyStatus(0)))
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (1)
    f.battle.takeAction(NullAction(f.battle.partyStatus(1)))
    
    f.battle.readyEntity should equal (None)
  }
  
  "Battle" should "use AI to automatically handle enemy actions" in {
    val f = new BattleTest.BattleFixture(aiOpt = Some(new RandomEnemyAI))
    
    f.battle.advanceTime(f.battle.baseTurnTime)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (1)
    f.battle.takeAction(NullAction(f.battle.partyStatus(1)))
    
    f.battle.readyEntity should equal (None)
  }
}