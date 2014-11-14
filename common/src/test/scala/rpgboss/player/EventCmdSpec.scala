package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._
import org.json4s.native.Serialization

class EventCmdSpec extends UnitSpec {
  "EventCmd" should "produce correct script for LockPlayerMovement" in {
    val e = LockPlayerMovement(Array(
      ShowText(Array("Hello")),
      SetInt("foo", 1)))

    e.toJs should deepEqual(Array(
      """game.setInt("playerMovementLocks", game.getInt("playerMovementLocks") + 1);""",
      """try {""",
      """  game.showText(["Hello"]);""",
      """  game.setInt("foo", 1);""",
      """} finally {""",
      """  game.setInt("playerMovementLocks", game.getInt("playerMovementLocks") - 1);""",
      """}"""
    ))
  }

  "EventCmd" should "produce correct script for ModifyParty" in {
    ModifyParty(true, 5).toJs should deepEqual(
      Array("game.modifyParty(true, 5);"))
  }

  "EventCmd" should "produce correct script for AddRemoveItem" in {
    AddRemoveItem(
        IntParameter(constant = 1),
        true,
        IntParameter(constant = 5)).toJs should deepEqual(
            Array("game.addRemoveItem(1, 5);"))

    AddRemoveItem(
        IntParameter(constant = 12),
        false,
        IntParameter(constant = 25)).toJs should deepEqual(
            Array("game.addRemoveItem(12, 25 * -1);"))
  }

  "EventCmd" should "produce correct script for ShowText" in {
    val e1 = ShowText(Array("Hello"))
    e1.toJs should deepEqual (Array(
      """game.showText(["Hello"]);"""))

    val e2 = ShowText(Array("Hello", "World"))
    e2.toJs should deepEqual (Array(
      """game.showText(["Hello", "World"]);"""))

    val e3 = ShowText(Array("Hello", "World", "Quote mark: \""))
    e3.toJs should deepEqual (Array(
      """game.showText(["Hello", "World", "Quote mark: \""]);"""))
  }

  "EventCmd" should "produce correct script in comma-decimal locales" in {
    val e = Teleport(MapLoc("mapname", 1.5f, 5.5f), 0)

    e.toJs should deepEqual (Array(
      """game.teleport("mapname", 1.500000, 5.500000, 0);"""))

    import java.util.Locale
    val defaultLocale = Locale.getDefault()

    Locale.setDefault(Locale.FRANCE)

    e.toJs should deepEqual (Array(
      """game.teleport("mapname", 1.500000, 5.500000, 0);"""))

    Locale.setDefault(defaultLocale)
  }

  "EventCmd" should "produce correct script for SetEventState" in {
    val e1 = SetEventState(EntitySpec(WhichEntity.THIS_EVENT.id), 5)
    e1.toJs should deepEqual(
      Array("game.setEventState(event.mapName(), event.id(), 5);"))
    val e2 = SetEventState(EntitySpec(WhichEntity.EVENT_ON_MAP.id, "", 20), 6)
    e2.toJs should deepEqual(
      Array("game.setEventState(event.mapName(), 20, 6);"))
    val e3 = SetEventState(EntitySpec(
        WhichEntity.EVENT_ON_OTHER_MAP.id, "foo", 1), 2)
    e3.toJs should deepEqual(
      Array("game.setEventState(\"foo\", 1, 2);"))
  }

  "EventCmd" should "produce correct script for IncrementEventState" in {
    IncrementEventState().toJs should deepEqual(
      Array("game.incrementEventState(event.id());"))
  }

  "EventCmd" should "produce correct script for MoveEvent" in {
    val e1 = MoveEvent(EntitySpec(WhichEntity.PLAYER.id), 1, 5, false, true)
    e1.toJs should deepEqual(
      Array("game.movePlayer(1.000000, 5.000000, false, true);"))

    val e2 =
      MoveEvent(EntitySpec(WhichEntity.THIS_EVENT.id, "", 0), 4, 1, true, true)
    e2.toJs should deepEqual(
      Array("game.moveEvent(event.id(), 4.000000, 1.000000, true, true);"))

    val e3 =
      MoveEvent(EntitySpec(
          WhichEntity.EVENT_ON_MAP.id, "", 10), 1, 5, false, false)
    e3.toJs should deepEqual(
      Array("game.moveEvent(10, 1.000000, 5.000000, false, false);"))
  }

  "EventCmd" should "should work with RunJs" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(RunJs(
          """game.setInt("one", 1); game.setInt("two", 2);"""))
      }

      def testScript() = {
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

  "EventCmd" should "deserialize legacy names correctly" in {
    implicit val formats = Serialization.formats(EventCmd.hints)
    val legacyJson = """
      [
        {
          "jsonClass":"ShowText",
          "lines":[
            "Hi"
          ]
        },
        {
          "jsonClass":"SetEvtState",
          "state":1
        }
      ]"""
    val result = Serialization.read[Array[EventCmd]](legacyJson)
    val expected: Array[EventCmd] =
      Array(ShowText(Array("Hi")),
          SetEventState(EntitySpec(WhichEntity.THIS_EVENT.id), 1))
    result should deepEqual (expected)
  }

  "EventCmd" should "deserialize legacy AddRemoveItem correctly" in {
    implicit val formats = Serialization.formats(EventCmd.hints)
    val legacyJson = """
      [
        {
          "jsonClass":"AddRemoveItem",
          "itemId":12,
          "add":true,
          "qty":5
        }
      ]"""
    val result = Serialization.read[Array[EventCmd]](legacyJson)
    val expected: Array[EventCmd] =
      Array(AddRemoveItem(
          IntParameter(constant = 12), true, IntParameter(constant = 5)))
    result should deepEqual (expected)
  }
}