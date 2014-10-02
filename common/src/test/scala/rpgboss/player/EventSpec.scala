package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.model.resource.ResourceConstants

class EventSpec extends UnitSpec {
  "Events" should "activate and run their script" in {
    val testIntValue = 42 // arbitrary

    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(SetInt("testKey", testIntValue))
      }

      def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f);
        scriptInterface.activateEvent(1, true)
        val retrievedInt = scriptInterface.getInt("testKey")

        waiter {
          retrievedInt should equal(testIntValue)
        }
      }
    }

    test.runTest()
  }

  "Events" should "switch states correctly" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        val states = Array(
          RpgEventState(cmds = Array(
            RunJs("""game.setInt("one", game.getInt("one") + 1);"""),
            SetEvtState(1))),
          RpgEventState(cmds = Array(
            RunJs("""game.setInt("two", game.getInt("two") + 1);"""))))
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 0, 0, states)
        )
      }

      def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f)
        scriptInterface.activateEvent(1, true)
        val firstRunIntOne = scriptInterface.getInt("one")
        val firstRunIntTwo = scriptInterface.getInt("two")
        scriptInterface.activateEvent(1, true)
        val secondRunIntOne = scriptInterface.getInt("one")
        val secondRunIntTwo = scriptInterface.getInt("two")

        waiter {
          firstRunIntOne should equal(1)
          firstRunIntTwo should equal(0)
          secondRunIntOne should equal(1)
          secondRunIntTwo should equal(1)
        }
      }
    }

    test.runTest()
  }
}