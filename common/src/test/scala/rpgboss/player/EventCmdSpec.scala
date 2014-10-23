package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._

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
    AddRemoveItem(1, true, 5).toJs should deepEqual(
      Array("game.addRemoveItem(1, 5);"))

    AddRemoveItem(12, false, 25).toJs should deepEqual(
      Array("game.addRemoveItem(12, -25);"))
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
    SetEventState(5).toJs should deepEqual(
      Array("game.setEventState(event.id(), 5);"))
  }

  "EventCmd" should "produce correct script for IncrementEventState" in {
    IncrementEventState().toJs should deepEqual(
      Array("game.incrementEventState(event.id());"))
  }

  "EventCmd" should "produce correct script for MoveEvent" in {
    val e1 = MoveEvent(EntitySpec(WhichEntity.PLAYER.id, 0), 1, 5, false, true)
    e1.toJs should deepEqual(
      Array("game.movePlayer(1.000000, 5.000000, false, true);"))

    val e2 =
      MoveEvent(EntitySpec(WhichEntity.THIS_EVENT.id, 0), 4, 1, true, true)
    e2.toJs should deepEqual(
      Array("game.moveEvent(event.id(), 4.000000, 1.000000, true, true);"))

    val e3 =
      MoveEvent(EntitySpec(WhichEntity.OTHER_EVENT.id, 10), 1, 5, false, false)
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
}