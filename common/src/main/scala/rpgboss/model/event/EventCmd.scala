package rpgboss.model.event

import EventCmd._
import rpgboss.model.MapLoc

trait EventCmd {
  def toJs(): List[String]
  override def toString = {
    val js = toJs()
    
    if(js.isEmpty) {
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
    classOf[Teleport]
  )
  
  def aryToJs(a: Array[String]) = a.map(strToJs(_)).mkString("[", ", ", "]")
  def strToJs(s: String) = """"%s"""".format(s.replaceAll("\"", "\\\\\""))
}

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def toJs() = List("game.showText(" + aryToJs(lines) + ");")
}

case class Teleport(loc: MapLoc, transition: Int) extends EventCmd {
  def toJs() = List("""game.teleport("%s", %f, %f, %d);""".format(
      loc.map, loc.x, loc.y, transition))
}

// A "None" in the evtName means for the current event
case class SetEvtState(
    evtNameOpt: Option[String], 
    newState: Int) extends EventCmd {
  def toJs() = {
    val evtName = evtNameOpt.map("\"%s\"".format(_)).getOrElse("event.name()")
    List("""game.setEvtState(%s, %d);""".format(evtName, newState))
  }
}