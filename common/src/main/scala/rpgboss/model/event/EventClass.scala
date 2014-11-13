package rpgboss.model.event

import rpgboss.model.HasName
import rpgboss.model.RpgEnum
import rpgboss.model.ProjectData

object EventParameterValueType extends RpgEnum {
  val Constant = Value(0)
  val Parameter = Value(1)

  def default = Constant
}

trait EventParameter {
  def jsString: String
}

case class IntParameter(
    var valueTypeId: Int = EventParameterValueType.Constant.id,
    var constant: Int = 0,
    var parameter: String = "") extends EventParameter {
  import EventParameterValueType._

  def jsString = EventParameterValueType(valueTypeId) match {
    case Constant => EventJavascript.toJs(constant)
    case Parameter => parameter
  }
}

case class EventClass(
  var name: String = "",
  var states: Array[RpgEventState] = Array(RpgEventState()),
  var params: Map[String, EventParameter] = Map()) extends HasName

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
  var params: Map[String, EventParameter])