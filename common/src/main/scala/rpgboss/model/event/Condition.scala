package rpgboss.model.event

import rpgboss.model.RpgEnum
import rpgboss.player.PersistentState
import rpgboss.player.PersistentState
import rpgboss.player.HasScriptConstants

object ComparisonOperator extends RpgEnum {
  case class Val(i: Int, name: String, comparator: (Int, Int) => Boolean)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val EQ = Val(0, "Equals", _ == _)
  val NE = Val(1, "NotEquals", _ != _)
  val LT = Val(2, "LessThan", _ < _)
  val LE = Val(3, "LessThanOrEquals", _ <= _)
  val GT = Val(4, "GreaterThan", _ > _)
  val GE = Val(5, "GreaterThanOrEquals", _ >= _)

  override def default = EQ
}

object ConditionType extends RpgEnum {
  val GlobalIntegerGreaterThanOrEquals = Value
  val HasItemsInInventory = Value
  val HasCharacterInParty = Value

  def default = GlobalIntegerGreaterThanOrEquals
}

case class Condition(
  conditionTypeId: Int,
  negate: Boolean,
  key: String,
  value1: Int,
  value2: Int)

//case class Condition(
//  var globalVariableConditions: Array[GlobalVariableCondition],
//  var requiredItemId: Option[Int],
//  var requiredCharacterId: Option[Int]) extends HasScriptConstants {
//  def evaluate(persistent: PersistentState) = {
//    globalVariableConditions.forall(_.evaluate(persistent)) &&
//    requiredItemId.forall(persistent.countItems(_) > 0) &&
//    requiredCharacterId.forall(persistent.getIntArray(PARTY).contains(_))
//  }
//}

case class GlobalVariableCondition(key: String, operatorId: Int, value: Int) {
  def evaluate(persistent: PersistentState) = {
    val operator = ComparisonOperator(operatorId)
    operator.comparator.apply(persistent.getInt(key), value)
  }
}