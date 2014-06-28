package rpgboss.model.event

import EventCmd._
import rpgboss.model._
import rpgboss.player._

trait EventCmd extends HasScriptConstants {
  def toJs(): Array[String]
  override def toString = {
    val js = toJs().toList

    if (js.isEmpty) {
      ">>> "
    } else {
      val lines = (">>> " + js.head) :: js.tail.map("... " + _)

      lines.mkString("\n")
    }
  }
}

object EventCmd {
  def types = List(
    classOf[EndOfScript],
    classOf[LockPlayerMovement],
    classOf[ShowText],
    classOf[Teleport],
    classOf[SetEvtState],
    classOf[MoveEvent],
    classOf[RunJs],
    classOf[StartBattle],
    classOf[SetInt])

  case class JsExp(exp: String)

  def toJs(x: Any): String = {
    import java.util.Locale
    x match {
      case JsExp(exp) =>
        exp
      case x: String =>
        """"%s"""".format(x.replaceAll("\"", "\\\\\""))
      case x: Array[String] =>
        x.map(toJs).mkString("[", ", ", "]")
      case x: Double =>
        "%f".formatLocal(Locale.US, x)
      case x: Float =>
        "%f".formatLocal(Locale.US, x)
      case x: Int =>
        "%d".formatLocal(Locale.US, x)
      case x: Long =>
        "%d".formatLocal(Locale.US, x)
      case x: Boolean =>
        "%b".formatLocal(Locale.US, x)
      case _ =>
        "undefined"
    }
  }

  def JsCall(functionName: String, args: Any*): JsExp = {
    val argsString = args.map(toJs).mkString(", ")
    JsExp("""%s(%s)""".format(functionName, argsString))
  }

  def JsStatement(functionName: String, args: Any*): String = {
    JsCall(functionName, args: _*).exp + ";"
  }

  def callJsToList(functionName: String, args: Any*): Array[String] = {
    Array(JsStatement(functionName, args: _*))
  }
}

case class EndOfScript() extends EventCmd {
  def toJs() = Array()
}

case class LockPlayerMovement(body: Array[EventCmd]) extends EventCmd {
  // TODO: Clean this up. Probably need a better JS DSL.
  def toJs() =
    Array(
      JsStatement("game.setInt", PLAYER_MOVEMENT_LOCKS,
        JsExp(JsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " + 1")),
      JsExp("try {").exp) ++
    body.flatMap(_.toJs()).map("  " + _) ++
    Array(
      JsExp("} finally {").exp,
      JsStatement("  game.setInt", PLAYER_MOVEMENT_LOCKS,
        JsExp(JsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " - 1")),
      JsExp("}").exp)
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def toJs() = callJsToList("game.showText", lines)
}

case class Teleport(loc: MapLoc, transition: Int) extends EventCmd {
  def toJs() = callJsToList("teleport", loc.map, loc.x, loc.y, transition)
}

case class SetEvtState(state: Int = 0) extends EventCmd {
  def toJs() = callJsToList("game.setEvtState", JsExp("event.idx()"), state)
}

case class MoveEvent(
  var entitySpec: EntitySpec = EntitySpec(),
  var dx: Float = 0f,
  var dy: Float = 0f,
  var affixDirection: Boolean = false,
  var async: Boolean = false) extends EventCmd {
  def toJs() = {
    entitySpec match {
      case EntitySpec(which, _) if which == WhichEntity.PLAYER.id =>
        callJsToList("game.movePlayer", dx, dy, affixDirection, async)
      case EntitySpec(which, _) if which == WhichEntity.THIS_EVENT.id =>
        callJsToList(
          "game.moveEvent", JsExp("event.id()"), dx, dy, affixDirection,
          async)
      case EntitySpec(which, eventIdx) if which == WhichEntity.OTHER_EVENT.id =>
        callJsToList(
          "game.moveEvent", entitySpec.eventId, dx, dy, affixDirection, async)
    }
  }
}

case class StartBattle(encounterId: Int = 0, battleBackground: String = "")
  extends EventCmd {
  def toJs() = callJsToList("game.startBattle", encounterId, battleBackground)
}

case class RunJs(scriptBody: String = "") extends EventCmd {
  def toJs() = Array(scriptBody.split("\n") : _*)
}

case class SetInt(key: String, value: Int) extends EventCmd {
  def toJs() = callJsToList("game.setInt", key, value)
}