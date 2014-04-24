package rpgboss.model.battle

import rpgboss._
import rpgboss.model._

object BattleTest {
  class BattleFixture() {
    val pData = ProjectData("fake-uuid", "fake-title")
    
    val characterFast = 
      Character(progressions = StatProgressions(spd = Curve(10, 2)))
    val characterSlow = 
      Character(progressions = StatProgressions(spd = Curve(4, 2)))
    val enemyMedium =
      Enemy(spd = 8)
      
    pData.enums.characters = Seq(characterFast, characterSlow)
    pData.enums.enemies = Seq(enemyMedium)
    
    val battle = new Battle(
      pData = pData, 
      partyIds = Seq(0, 1), 
      characterLevels = Seq(1, 1),
      initialCharacterHps = Seq(1, 1),
      initialCharacterMps = Seq(1, 1),
      characterEquip = Seq(Seq(), Seq()),
      initialCharacterTempStatusEffects = Seq(Seq(), Seq()),
      characterRows = Seq(0, 0),
      encounter = Encounter(units = Seq(EncounterUnit(0, 100, 100))))
  }
}

class BattleSpec extends UnitSpec {
  def fixture = new BattleTest.BattleFixture
  
  "Battle" should "make fastest unit go first" in {
    val f = fixture
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    
    f.battle.readyEntity should equal (None)
  }
  
  "Battle" should "have battle units act in order of speed" in {
    val f = fixture
    
    f.battle.update(f.battle.baseTurnTime)
    
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
}