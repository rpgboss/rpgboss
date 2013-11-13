package rpgboss.model

import rpgboss._
import com.google.common.io.Files

class BattleStatsSpec extends UnitSpec {
  def newPData = {
    val pData = ProjectData("fake-uuid", "fake-title")
    
    val statProgressions = StatProgressions(
      exp = Curve(300, 100),
      mhp = Curve(50, 10),
      mmp = Curve(20, 4),
      atk = Curve(10, 2),
      spd = Curve(10, 2),
      mag = Curve(10, 2))
    val character = Character(progressions = statProgressions)
    
    pData.enums.characters = Seq(character)
    
    pData
  }
  
  "BattleStats" should "work with stat progressions" in {
    val pData = newPData
    
    val stats1 = BattleStats(pData, 0, level = 1)
    stats1 should equal (BattleStats(50, 20, 10, 10, 10, Seq()))
    
    val stats11 = BattleStats(pData, 0, level = 11)
    stats11 should equal (BattleStats(150, 60, 30, 30, 30, Seq()))
  }
  
  "BattleStats" should "work with status effects" in {
    val pData = newPData
    pData.enums.statusEffects = Seq(StatusEffect(
      effects = Seq(Effect(EffectKey.AtkAdd.id, 10))))
    
    val stats1 = BattleStats(pData, 0, level = 1, otherStatusEffectIds = Seq(0))
    stats1 should equal (BattleStats(
      50, 20, 20, 10, 10, Seq(pData.enums.statusEffects(0))))
  }

  "BattleStats" should "work with equipment effects" in {
    val pData = newPData
    pData.enums.items = 
      Seq(Item(effects = Seq(Effect(EffectKey.AtkAdd.id, 10))))
    
    val stats1 = BattleStats(pData, 0, level = 1, equippedIds = Seq(0))
    stats1 should equal (BattleStats(50, 20, 20, 10, 10, Seq()))
  }
  
  "BattleStats" should "work with equipment status effects" in {
    val pData = newPData
    pData.enums.statusEffects = Seq(StatusEffect(
      effects = Seq(Effect(EffectKey.AtkAdd.id, 10))))
    pData.enums.items = 
      Seq(Item(effects = Seq(Effect(EffectKey.AddStatusEffect.id, 0))))
    
    val stats1 = BattleStats(pData, 0, level = 1, equippedIds = Seq(0))
    stats1 should equal (BattleStats(
      50, 20, 20, 10, 10, Seq(pData.enums.statusEffects(0))))
  }
  
  
}