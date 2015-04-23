package rpgboss.model.event

import rpgboss.model.HasName
import rpgboss.model.RpgEnum
import rpgboss.model.ProjectData
import org.json4s.ShortTypeHints
import rpgboss.model.ProjectDataEnums

object OperatorType extends RpgEnum {
  case class Val(i: Int, name: String, jsString: String)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val Set = Val(0, "Set_to", "")
  val Add = Val(1, "Add_by", "+")
  val Substract = Val(2, "Substract_by", "-")
  val Multiply = Val(3, "Multiply_with", "*")
  val Divide = Val(4, "Divide_with", "/")
  val Mod = Val(5, "Mod_with", "%")

  override def default = Set
}

object EventParameterValueType extends RpgEnum {
  val Constant = Value(0, "Constant")
  val LocalVariable = Value(1, "Local_Variable")
  val GlobalVariable = Value(2, "Global_Variable")

  def default = Constant
}

trait EventParameter[T] {
  def valueTypeId: Int
  def valueTypeId_=(v: Int)

  def constant: T
  def constant_=(v: T)

  def localVariable: String
  def localVariable_=(v: String)

  def supportsGlobalVariable = false
  def globalVariable: String = {
    "Error: Global Variables unsupported for this type."
  }
  def globalVariable_=(v: String): Unit = {
    throw new RuntimeException("Global Variable not supported for this type.")
  }

  def copyValuesFrom(other: EventParameter[T]) = {
    valueTypeId = other.valueTypeId
    constant = other.constant
    localVariable = other.localVariable
    if (supportsGlobalVariable && other.supportsGlobalVariable)
      globalVariable = other.globalVariable
  }

  import EventParameterValueType._

  def rawJs: RawJs = EventParameterValueType(valueTypeId) match {
    case Constant => RawJs(EventJavascript.toJs(constant))
    case LocalVariable => RawJs(localVariable)
    case GlobalVariable => RawJs("GLOBAL_VARIABLES_UNSUPPORTED")
  }
}

object EventParameter {
  val hints = ShortTypeHints(List(
    classOf[BooleanParameter],
    classOf[FloatParameter],
    classOf[IntArrayParameter],
    classOf[IntParameter],
    classOf[StringParameter]))
}

case class BooleanParameter(
    var constant: Boolean = false,
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var localVariable: String = "") extends EventParameter[Boolean]

case class FloatParameter(
    var constant: Float = 0,
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var localVariable: String = "") extends EventParameter[Float]

case class IntArrayParameter(
    var constant: Array[Int] = Array(),
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var localVariable: String = "") extends EventParameter[Array[Int]]

case class IntParameter(
    var constant: Int = 0,
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var localVariable: String = "",
    override var globalVariable: String = "") extends EventParameter[Int] {
  override def supportsGlobalVariable = true

  override def rawJs =  EventParameterValueType(valueTypeId) match {
    case EventParameterValueType.GlobalVariable =>
      EventJavascript.jsCall("game.getInt", globalVariable)
    case _ => super.rawJs
  }
}

object IntParameter {
  def globalVariable(key: String = "globalVariableName") =
    IntParameter(
        valueTypeId = EventParameterValueType.GlobalVariable.id,
        globalVariable = key)
}

case class StringParameter(
  var constant: String = "",
  var valueTypeId: Int = EventParameterValueType.Constant.id,
  var localVariable: String = "",
  override var globalVariable: String = "") extends EventParameter[String] {
  override def supportsGlobalVariable = true

  override def rawJs =  EventParameterValueType(valueTypeId) match {
    case EventParameterValueType.GlobalVariable =>
      EventJavascript.jsCall("game.getString", globalVariable)
    case _ => super.rawJs
  }
}

case class EventClass(
  var name: String = "",
  var states: Array[RpgEventState] = Array(RpgEventState())) extends HasName