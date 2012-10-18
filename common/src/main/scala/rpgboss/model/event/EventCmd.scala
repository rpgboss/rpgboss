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

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
}

object EventCmd {
  def types = List(
      classOf[EndOfScript]
      )
}