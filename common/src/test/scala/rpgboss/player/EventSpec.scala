package rpgboss.player

import rpgboss._

class EventSpec extends UnitSpec {
  "Events" should "activate and run their script" in {
    val testIntValue = 42 // arbitrary
    
    val test = new BasicTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        val sprite = SpriteSpec("vx_chara02_a.png", 0)
        val cmds: Array[EventCmd] = Array(SetInt("testKey", testIntValue))
        val states = Array(RpgEventState(sprite = Some(sprite), cmds = cmds))
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 2f, 2f, states)
        )
      }
      
      def testScript() = {
        scriptInterface.setPlayerLoc(MapLoc(mapName, 0.5f, 0.5f));
        scriptInterface.activateEvent(1, true)
        val retrievedInt = scriptInterface.getInt("testKey")
        
        waiter {
          retrievedInt should equal(testIntValue)
        }
      }
    }
    
    test.runTest()
  }
}