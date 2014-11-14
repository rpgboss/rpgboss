package rpgboss.model.event

import rpgboss.model.HasName
import rpgboss.model.RpgEnum
import rpgboss.model.ProjectData
import org.json4s.ShortTypeHints

object EventParameterValueType extends RpgEnum {
  val Constant = Value(0, "Constant")
  val LocalVariable = Value(1, "Local Variable")

  def default = Constant
}

trait EventParameter[T] {
  def jsString: String

  def valueTypeId: Int
  def valueTypeId_=(v: Int)

  def constant: T
  def constant_=(v: T)

  def localVariable: String
  def localVariable_=(v: String)

  def copyValuesFrom(other: EventParameter[T]) = {
    valueTypeId = other.valueTypeId
    constant = other.constant
    localVariable = other.localVariable
  }
}

object EventParameter {
  val hints = ShortTypeHints(List(
    classOf[IntParameter]))
}

case class IntParameter(
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var constant: Int = 0,
    var localVariable: String = "") extends EventParameter[Int] {
  import EventParameterValueType._

  def jsString = EventParameterValueType(valueTypeId) match {
    case Constant => EventJavascript.toJs(constant)
    case LocalVariable => localVariable
  }
}

case class EventClass(
  var name: String = "",
  var states: Array[RpgEventState] = Array(RpgEventState())) extends HasName

/**
 * This information combined with an EventClass can be used to instantiate an
 * RpgEvent.
 */
case class EventInstance(
  var eventClassId: Int,
  id: Int,
  var name: String,
  var x: Float,
  var y: Float,
  var params: Map[String, EventParameter[_]])