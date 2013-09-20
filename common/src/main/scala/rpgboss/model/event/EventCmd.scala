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

object WhichEvent extends RpgEnum {
  val PLAYER = Value("Player")
  val THISEVENT = Value("This event")
  val SAMEMAPEVENT = Value("Other event on same map")
  val OTHERMAPEVENT = Value("Other event on other map")

  def default = THISEVENT
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

// A "None" in the evtName means for the current event
case class SetEvtState(
  whichEvtId: Int = WhichEvent.default.id,
  evtPathOpt: Option[EvtPath] = None,
  state: Int = 0) extends EventCmd {
  def toJs() = {
    import WhichEvent._
    val cmd = WhichEvent(whichEvtId) match {
      case THISEVENT =>
        """game.setEvtState(%s, %d);""".format("event.name()", state)
      case SAMEMAPEVENT =>
        """game.setEvtState(event.mapName(), %s, %d);"""
          .format(evtPathOpt.get.evtName, state)
      case OTHERMAPEVENT =>
        """game.setEvtState("%s", "%s", %d);"""
          .format(evtPathOpt.get.mapName, evtPathOpt.get.evtName, state)
    }

    List(cmd)
  }
}

case class MoveEvent(
  whichEvtId: Int = WhichEvent.default.id,
  evtPathOpt: Option[EvtPath] = None,
  dx: Float,
  dy: Float,
  changeDirection: Boolean,
  awaitDone: Boolean) extends EventCmd {
  def toJs() = {
    import WhichEvent._
    val getEntityCmd = WhichEvent(whichEvtId) match {
      case PLAYER =>
        """var _entity = game.getPlayerEntity();"""
      case THISEVENT =>
        """var _entity = game.getEventEntity(%s);""".format("event.name()")
      case SAMEMAPEVENT =>
        """var _entity = game.getEventEntity(%s);""".format(
          evtPathOpt.get.evtName)
    }
    
    val moveCmd = """game.moveEntity(_entity, %f, %f, %b, %b);""".format(
      dx, dy, changeDirection, awaitDone)

    List(getEntityCmd, moveCmd)
  }
}