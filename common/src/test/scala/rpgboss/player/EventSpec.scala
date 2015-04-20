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
        mapData.events = singleTestEvent(
            SetGlobalInt("testKey", value1 = IntParameter(testIntValue)))
      }

      override def testScript() = {
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
          1->RpgEvent(1, "Testevent", 0, 0, states)
        )
      }

      override def testScript() = {
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
          1->RpgEvent(1, "Testevent", 0, 0, states)
        )
      }

      override def testScript() = {
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

  "Events" should "show text correctly" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        val states = Array(
          RpgEventState(cmds = Array(
            ShowText(Array("Hello")),
            RunJs("""game.setInt("one", game.getInt("one") + 1);"""))))
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 0, 0, states)
        )
      }

      override def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f)

        val a1 = scriptInterface.getInt("one")
        scriptInterface.activateEvent(1, false)
        scriptInterface.sleep(1.0f)  // Wait for text box to show up
        scriptInterface.mapScreenKeyPress(MyKeys.OK)
        val a2 = scriptInterface.getInt("one")

        waiter {
          a1 should equal (0)
          a2 should equal (1)
        }
      }
    }

    test.runTest()
  }

  "Events" should "work with RunJs" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(RunJs(
          """game.setInt("one", 1); game.setInt("two", 2);"""))
      }

      override def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f);
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