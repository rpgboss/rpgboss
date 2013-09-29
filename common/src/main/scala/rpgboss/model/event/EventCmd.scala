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
    classOf[SetEvtState])

  def aryToJs(a: Array[String]) = a.map(strToJs(_)).mkString("[", ", ", "]")
  def strToJs(s: String) = """"%s"""".format(s.replaceAll("\"", "\\\\\""))
}

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def toJs() = List("showText(" + aryToJs(lines) + ");")
}

case class Teleport(loc: MapLoc, transition: Int) extends EventCmd {
  def toJs() = List("""teleport("%s", %f, %f, %d);""".format(
    loc.map, loc.x, loc.y, transition))
}

case class SetEvtState(state: Int = 0) extends EventCmd {
  def toJs() =
    List("""game.setEvtState(%s, %d);""".format("event.idx()", state))
}

case class MoveEvent(
  var entitySpec: EntitySpec,
  var dx: Float,
  var dy: Float,
  var changeDirection: Boolean,
  var awaitDone: Boolean) extends EventCmd {
  def toJs() = {
    val getEntityCmd = entitySpec match {
      case EntitySpec(WhichEntity.PLAYER, _) =>
        """var _entity = game.getPlayerEntity();"""
      case EntitySpec(WhichEntity.THIS_EVENT, _) =>
        """var _entity = game.getEventEntity(%s);""".format("event.name()")
      case EntitySpec(WhichEntity.OTHER_EVENT, eventIdx) =>
        """var _entity = game.getEventEntity(%s);""".format(eventIdx)
    }
    
    val moveCmd = """game.moveEntity(_entity, %f, %f, %b, %b);""".format(
      dx, dy, changeDirection, awaitDone)

    List(getEntityCmd, moveCmd)
  }
}