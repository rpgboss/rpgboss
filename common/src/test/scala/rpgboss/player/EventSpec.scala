package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._

class EventSpec extends UnitSpec {
  "Events" should "activate and run their script" in {
    val testIntValue = 42 // arbitrary
    
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(SetInt("testKey", testIntValue))
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