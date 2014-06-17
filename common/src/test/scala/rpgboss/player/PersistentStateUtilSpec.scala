package rpgboss.player

import rpgboss.UnitSpec
import rpgboss.model._
import rpgboss.player.PersistentStateUtil._

class PersistentStateUtilSpec extends UnitSpec {
  def fixture(level1: Int, level2: Int, exp1: Int, exp2: Int) = new {
    val character1 = 
      Character(progressions = StatProgressions(exp = Curve(300, 100)))
    val character2 = 
      Character(progressions = StatProgressions(exp = Curve(600, 200)))
    val characters = Array(character1, character2)
    
    import ScriptInterfaceConstants._
    
    val persistent = new PersistentState
    persistent.setIntArray(CHARACTER_LEVELS , Array(level1, level2))
    persistent.setIntArray(CHARACTER_EXPS, Array(exp1, exp2))
    
    def getState() = new {
      val levels = persistent.getIntArray(CHARACTER_LEVELS)
      val exps = persistent.getIntArray(CHARACTER_EXPS)
    }
        
  }
  
  "PersistentStateUtil" should "level up specified character only" in {
    val f = fixture(1, 1, 0, 0)
    val leveled = givePartyExperience(f.persistent, f.characters, Array(1), 700)
    val state = f.getState()
    
    leveled should deepEqual (Array(1))
    state.levels should deepEqual (Array(1, 2))
    state.exps should deepEqual (Array(0, 100))
  }
  
  "PersistentStateUtil" should "level up both characters" in {
    val f = fixture(1, 1, 0, 0)
    val leveled = 
      givePartyExperience(f.persistent, f.characters, Array(0, 1), 600)
    val state = f.getState()
    
    leveled should deepEqual (Array(0, 1))
    state.levels should deepEqual (Array(2, 2))
    state.exps should deepEqual (Array(300, 0))
  }
  
  "PersistentStateUtil" should "level up through multiple levels" in {
    val f = fixture(1, 1, 0, 0)
    val leveled = 
      givePartyExperience(f.persistent, f.characters, Array(0, 1), 700)
    val state = f.getState()
    
    leveled should deepEqual (Array(0, 1))
    state.levels should deepEqual (Array(3, 2))
    state.exps should deepEqual (Array(0, 100))
  }
}