package rpgboss.model.event

import EventCmd._
import rpgboss.model._

trait EventCmd {
  def toJs(): List[String]
  override def toString = {
    val js = toJs()

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

  def callJs(functionName: String, args: Any*) = {
    val argsString = args.map(toJs).mkString(", ")
    """%s(%s);""".format(functionName, argsString)
  }

  def callJsToList(functionName: String, args: Any*) = {
    List(callJs(functionName, args: _*))
  }
}

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
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
  def toJs() = List(scriptBody)
}

case class SetInt(key: String, value: Int) extends EventCmd {
  def toJs() = callJsToList("game.setInt", key, value)
}