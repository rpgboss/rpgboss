package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.model.event._
import org.json4s.native.Serialization

class EventCmdSpec extends UnitSpec {
  "EventCmd" should "produce correct script for AddRemoveItem" in {
    AddRemoveItem(true, IntParameter(1), IntParameter(5)).toJs should deepEqual(
        Array("game.addRemoveItem(1, 5);"))

    AddRemoveItem(
        false, IntParameter(12), IntParameter(25)).toJs should deepEqual(
        Array("game.addRemoveItem(12, 25 * -1);"))
  }

  "EventCmd" should "produce correct script for AddRemoveGold" in {
    AddRemoveGold(true, IntParameter(12)).toJs should deepEqual(
        Array("game.addRemoveGold(12);"))

    AddRemoveGold(false, IntParameter(25)).toJs should deepEqual(
        Array("game.addRemoveGold(25 * -1);"))
  }

  "EventCmd" should "produce correct script for GetChoice" in {
    val e = GetChoice(Array("Question text"), Array("Yes", "No"), true,
        Array(
            Array(ShowText(Array("Hello"))),
            Array(ShowText(Array("Goodbye"))),
            Array(ShowText(Array("Default")))))

    e.toJs should deepEqual(Array(
      """switch (game.getChoice(["Question text"], ["Yes", "No"], true, {})) {""",
      """  case 0:""",
      """    game.showText(["Hello"]);""",
      """    break;""",
      """  case 1:""",
      """    game.showText(["Goodbye"]);""",
      """    break;""",
      """  default:""",
      """    game.showText(["Default"]);""",
      """    break;""",
      """}"""
    ))
  }

  "EventCmd" should "produce correct script for HidePicture" in {
    HidePicture(IntParameter(5)).toJs should deepEqual(
        Array("game.hidePicture(5);"))
  }

  "EventCmd" should "produce correct script for IfCondition" in {
    val e1 = IfCondition(Array(), false, Array(ShowText(Array("a"))), Array())

    e1.toJs should deepEqual(Array(
      """if (true) {""",
      """  game.showText(["a"]);""",
      """}"""
    ))

    val e2 = IfCondition(Array(), true, Array(ShowText(Array("a"))),
        Array(ShowText(Array("b"))))

    e2.toJs should deepEqual(Array(
      """if (true) {""",
      """  game.showText(["a"]);""",
      """} else {""",
      """  game.showText(["b"]);""",
      """}"""
    ))
  }

  "EventCmd" should "produce correct script for LockPlayerMovement" in {
    val e = LockPlayerMovement(Array(
      ShowText(Array("Hello")),
      SetGlobalInt("foo", value1 = IntParameter(1))))

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

  "EventCmd" should "produce correct script for IncrementEventState" in {
    IncrementEventState().toJs should deepEqual(
      Array("game.incrementEventState(event.id());"))
  }

  "EventCmd" should "produce correct script for MoveEvent" in {
    val e1 = MoveEvent(EntitySpec(WhichEntity.PLAYER.id), 1, 5, false, true)
    e1.toJs should deepEqual(
      Array("game.movePlayer(1.0, 5.0, false, true);"))

    val e2 =
      MoveEvent(EntitySpec(WhichEntity.THIS_EVENT.id, "", 0), 4, 1, true, true)
    e2.toJs should deepEqual(
      Array("game.moveEvent(event.id(), 4.0, 1.0, true, true);"))

    val e3 =
      MoveEvent(EntitySpec(
          WhichEntity.EVENT_ON_MAP.id, "", 10), 1, 5, false, false)
    e3.toJs should deepEqual(
      Array("game.moveEvent(10, 1.0, 5.0, false, false);"))
  }

  "EventCmd" should "produce correct script for PlayMusic" in {
    val e1 = PlayMusic(IntParameter(5), SoundSpec("test.mp3", 1.2f, 1.3f),
        false, 0.4f)
    e1.toJs should deepEqual(
      Array("game.playMusic(5, \"test.mp3\", 1.2, false, 0.4);"))
  }

  "EventCmd" should "produce correct script for PlaySound" in {
    val e1 = PlaySound(SoundSpec("test.mp3", 1.2f, 1.3f))
    e1.toJs should deepEqual(
      Array("game.playSound(\"test.mp3\", 1.2, 1.3);"))
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

  "EventCmd" should "produce correct script for SetGlobalInt" in {
    val e1 = SetGlobalInt("foo", OperatorType.Set.id,
        IntParameter(42))
    e1.toJs should deepEqual(
      Array("game.setInt(\"foo\", 42);"))
    val e2 = SetGlobalInt("bar", OperatorType.Add.id, IntParameter(12))
    e2.toJs should deepEqual(
      Array("game.setInt(\"bar\", game.getInt(\"bar\") + 12);"))
    val e3 = SetGlobalInt("foo", OperatorType.Add.id,
        IntParameter(
            valueTypeId = EventParameterValueType.LocalVariable.id,
            localVariable = "bar"))
    e3.toJs should deepEqual(
      Array("game.setInt(\"foo\", game.getInt(\"foo\") + bar);"))
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

    val e4 = ShowText(Array("Hello \\J[getItemName(itemId)]"))
    e4.toJs should deepEqual (Array(
      """game.showText(["Hello " + getItemName(itemId) + ""]);"""))
  }

  "EventCmd" should "produce correct script for StopMusic" in {
    val e1 = StopMusic(IntParameter(12), 0.8f)
    e1.toJs should deepEqual(
      Array("game.stopMusic(12, 0.8);"))
  }

  "EventCmd" should "render IntParameters correctly" in {
    IntParameter(12).rawJs.exp should equal ("12")
    IntParameter(
        valueTypeId = EventParameterValueType.LocalVariable.id,
        localVariable = "foo").rawJs.exp should equal ("foo")
    IntParameter(
        valueTypeId = EventParameterValueType.GlobalVariable.id,
        globalVariable = "bar").rawJs.exp should equal ("game.getInt(\"bar\")")
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

  "EventCmd" should "produce correct script in comma-decimal locales" in {
    val e = Teleport(MapLoc("mapname", 1.5f, 5.5f), 0)

    e.toJs should deepEqual (Array(
      """game.teleport("mapname", 1.5, 5.5, 0);"""))

    import java.util.Locale
    val defaultLocale = Locale.getDefault()

    Locale.setDefault(Locale.FRANCE)

    e.toJs should deepEqual (Array(
      """game.teleport("mapname", 1.5, 5.5, 0);"""))

    Locale.setDefault(defaultLocale)
  }
}