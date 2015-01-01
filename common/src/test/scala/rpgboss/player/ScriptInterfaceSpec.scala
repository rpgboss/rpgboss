package rpgboss.player

import rpgboss._
import rpgboss.model.SoundSpec

class ScriptInterfaceSpec extends UnitSpec {
  "game.playSound call" should "work" in {
    val test = new MapScreenTest {
      override def testScript() = {
        scriptInterface.playSound("sys/rpgboss-menu/MenuSelect.mp3")
        scriptInterface.playSound("")
        scriptInterface.playSound("nonexistent.mp3")
      }
    }

    test.runTest()
  }

  "game.modifyParty call" should "work" in {
    val test = new MapScreenTest {
      override def testScript() = {
        val party0 = scriptInterface.getIntArray(PARTY)
        waiter {
          party0 should deepEqual(Array(0))
        }

        // Test adding existing party member
        val result1 = scriptInterface.modifyParty(add = true, characterId = 0)
        val party1 = scriptInterface.getIntArray(PARTY)
        waiter {
          result1 should equal(false)
          party1 should deepEqual(Array(0))
        }

        // Test removing non-existing party member
        val result2 = scriptInterface.modifyParty(add = false, characterId = 1)
        val party2 = scriptInterface.getIntArray(PARTY)
        waiter {
          result2 should equal(false)
          party2 should deepEqual(Array(0))
        }

        // Test adding non-existing party member
        val result3 = scriptInterface.modifyParty(add = true, characterId = 1)
        val party3 = scriptInterface.getIntArray(PARTY)
        waiter {
          result3 should equal(true)
          party3 should deepEqual(Array(0, 1))
        }

        // Test removing existing party member
        val result4 = scriptInterface.modifyParty(add = false, characterId = 0)
        val party4 = scriptInterface.getIntArray(PARTY)
        waiter {
          result4 should equal(true)
          party4 should deepEqual(Array(1))
        }
      }
    }

    test.runTest()
  }
}