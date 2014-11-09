package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.model.resource._

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
            IncrementEventState())),
          RpgEventState(cmds = Array(
            RunJs("""game.setInt("two", game.getInt("two") + 1);"""),
            SetEventState(EntitySpec(WhichEntity.THIS_EVENT.id), 0))))
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 0, 0, states, Map())
        )
      }

      def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f)
        def getIntTuple() =
          (scriptInterface.getInt("one"), scriptInterface.getInt("two"))

        scriptInterface.activateEvent(1, true)
        val (a1, a2) = getIntTuple()
        scriptInterface.activateEvent(1, true)
        val (b1, b2) = getIntTuple()
        scriptInterface.activateEvent(1, true)
        val (c1, c2) = getIntTuple()


        waiter {
          (a1, a2) should equal (1, 0)
          (b1, b2) should equal (1, 1)
          (c1, c2) should equal (2, 1)
        }
      }
    }

    test.runTest()
  }

  "Events" should "respect run once option" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        val states = Array(
          RpgEventState(runOnceThenIncrementState = true),
          RpgEventState())
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 0, 0, states, Map())
        )
      }

      def testScript() = {
        val mapName = RpgMap.generateName(project.data.lastCreatedMapId)
        val s1 = scriptInterface.getEventState(mapName, 1)
        scriptInterface.activateEvent(1, true)
        val s2 = scriptInterface.getEventState(mapName, 1)

        waiter {
          s1 should equal (0)
          s2 should equal (1)
        }
      }
    }

    test.runTest()
  }
}