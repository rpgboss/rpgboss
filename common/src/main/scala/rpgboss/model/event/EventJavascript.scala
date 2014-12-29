package rpgboss.model.event

trait CodeSection {
  def toJs: Array[String]
}

case class PlainLines(lines: Array[String]) extends CodeSection {
  def toJs = lines
}

case class RawJs(exp: String)

object EventJavascript {
  def toJs(x: Any): String = {
    import java.util.Locale
    x match {
      case RawJs(exp) =>
        exp
      case x: EventParameter[_] =>
        x.rawJs.exp
      case x: String =>
        """"%s"""".format(x.replaceAll("\"", "\\\\\""))
      case x: Array[_] =>
        x.map(toJs).mkString("[", ", ", "]")
      case x: Double =>
        "%s".formatLocal(Locale.US, x)
      case x: Float =>
        "%s".formatLocal(Locale.US, x)
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

  def applyOperator(
      operand1: RawJs,
      operator: String,
      operand2: RawJs): RawJs =
        RawJs("%s %s %s".format(
            operand1.exp,
            operator,
            operand2.exp))
}