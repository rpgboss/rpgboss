package rpgboss.model.event

import EventCmd._
import rpgboss.model._
import rpgboss.player._

trait EventCmd extends HasScriptConstants {
  def sections: Array[CodeSection]

  val toJs: Array[String] =
    sections.flatMap(_.toJs)

  override def toString: String = {
    if (toJs.isEmpty) {
      return ">>> "
    } else {
      val js = toJs.toList
      val lines = Array(">>> " + js.head) ++ js.tail.map("... " + _)
      lines.mkString("\n")
    }
  }
}

object EventCmd {
  trait CodeSection {
    def toJs: Array[String]
  }

  case class PlainLines(lines: Array[String]) extends CodeSection {
    def toJs = lines
  }

  case class CommandList(cmds: Array[EventCmd],
                         indent: Int) extends CodeSection {
    def toJs = cmds
      .map(_.toJs) // Convert to JS
      .flatten
      .map(("  " * indent) + _)  // Indent by 2 spaces
  }

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

  case class RawJs(exp: String)

  def toJs(x: Any): String = {
    import java.util.Locale
    x match {
      case RawJs(exp) =>
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

  def jsCall(functionName: String, args: Any*): RawJs = {
    val argsString = args.map(toJs).mkString(", ")
    RawJs("""%s(%s)""".format(functionName, argsString))
  }

  def jsStatement(functionName: String, args: Any*): String = {
    jsCall(functionName, args: _*).exp + ";"
  }

  def singleCall(functionName: String, args: Any*): Array[CodeSection] = {
    Array(PlainLines(Array(jsStatement(functionName, args: _*))))
  }
}

case class EndOfScript() extends EventCmd {
  def sections = Array()
}

case class LockPlayerMovement(body: Array[EventCmd]) extends EventCmd {
  // TODO: Clean this up. Probably need a better JS DSL.
  def sections = Array(
    PlainLines(Array(
      jsStatement("game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " + 1")),
      RawJs("try {").exp)),
    CommandList(body, 1),
    PlainLines(Array(
      RawJs("} finally {").exp,
      jsStatement("  game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " - 1")),
      RawJs("}").exp))
  )

}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def sections = singleCall("game.showText", lines)
}

case class Teleport(loc: MapLoc, transition: Int) extends EventCmd {
  def sections = singleCall("teleport", loc.map, loc.x, loc.y, transition)
}

case class SetEvtState(state: Int = 0) extends EventCmd {
  def sections = singleCall("game.setEvtState", RawJs("event.idx()"), state)
}

case class MoveEvent(
  var entitySpec: EntitySpec = EntitySpec(),
  var dx: Float = 0f,
  var dy: Float = 0f,
  var affixDirection: Boolean = false,
  var async: Boolean = false) extends EventCmd {
  def sections = {
    entitySpec match {
      case EntitySpec(which, _) if which == WhichEntity.PLAYER.id =>
        singleCall("game.movePlayer", dx, dy, affixDirection, async)
      case EntitySpec(which, _) if which == WhichEntity.THIS_EVENT.id =>
        singleCall(
          "game.moveEvent", RawJs("event.id()"), dx, dy, affixDirection,
          async)
      case EntitySpec(which, eventIdx) if which == WhichEntity.OTHER_EVENT.id =>
        singleCall(
          "game.moveEvent", entitySpec.eventId, dx, dy, affixDirection, async)
    }
  }
}

case class StartBattle(encounterId: Int = 0, battleBackground: String = "")
  extends EventCmd {
  def sections = singleCall("game.startBattle", encounterId, battleBackground)
}

case class RunJs(scriptBody: String = "") extends EventCmd {
  def sections = Array(PlainLines(Array(scriptBody.split("\n") : _*)))
}

case class SetInt(key: String, value: Int) extends EventCmd {
  def sections = singleCall("game.setInt", key, value)
}