package rpgboss.model.battle

import rpgboss._
import rpgboss.model._

class BattleSpec extends UnitSpec {
  def fixture(commanders: Seq[BattleCommander]) = new {
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
      encounter = Encounter(units = Seq(EncounterUnit(0, 100, 100))),
      commanders = commanders)
  }
  
  "Battle" should "make fastest unit go first" in {
    val f = fixture(Nil)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    
    f.battle.readyEntity should equal (None)
  }
  
  "Battle" should "have actions proceed in order" in {
    val f = fixture(Nil)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (0)
    
    f.battle.takeAction(NullAction(f.battle.partyStatus(0)))
    f.battle.readyEntity should equal (None)
    
    f.battle.update(f.battle.readySeparation)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Enemy)
    f.battle.readyEntity.get.id should equal (0)
    
    f.battle.takeAction(NullAction(f.battle.enemyStatus(0)))
    f.battle.readyEntity should equal (None)
    
    f.battle.update(f.battle.readySeparation)
    
    f.battle.readyEntity should be ('isDefined)
    f.battle.readyEntity.get.entityType should equal (BattleEntityType.Party)
    f.battle.readyEntity.get.id should equal (1)
    
    f.battle.takeAction(NullAction(f.battle.partyStatus(1)))
    f.battle.readyEntity should equal (None)
  }
}