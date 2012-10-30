package rpgboss.model.event

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
    classOf[ShowText]
  )
  
  def aryToJs[T](a: Array[T]) = a.mkString("[", ", ", "]") 
}

import EventCmd._

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def toJs() = List("showText(" + aryToJs(lines) + ");")
}

