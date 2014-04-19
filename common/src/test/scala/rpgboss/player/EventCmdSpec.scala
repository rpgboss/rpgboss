package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._

class EventCmdSpec extends UnitSpec {
  "EventCmd" should "should work with RunJs" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(RunJs(
          """game.setInt("one", 1); game.setInt("two", 2);"""))
      }
      
      def testScript() = {
        scriptInterface.setPlayerLoc(MapLoc(mapName, 0.5f, 0.5f));
        scriptInterface.activateEvent(1, true)
        val oneVal = scriptInterface.getInt("one")
        val twoVal = scriptInterface.getInt("two")
        
        waiter {
          oneVal should equal (1)
          twoVal should equal (2)
        }
      }
    }
    
    test.runTest()
  }
}