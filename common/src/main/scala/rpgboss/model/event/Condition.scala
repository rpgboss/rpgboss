package rpgboss.model.event

import rpgboss.model.RpgEnum
import rpgboss.player.PersistentState
import rpgboss.player.PersistentState
import rpgboss.player.HasScriptConstants

object ComparisonOperator extends RpgEnum {
  case class Val(i: Int, name: String, jsOperator: String)
    extends super.Val(i, name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val EQ = Val(0, "Equals", "==")
  val NE = Val(1, "NotEquals", "!=")
  val LT = Val(2, "LessThan", "<")
  val LE = Val(3, "LessThanOrEquals", "<=")
  val GT = Val(4, "GreaterThan", ">")
  val GE = Val(5, "GreaterThanOrEquals", ">=")

  override def default = EQ
}

object ConditionType extends RpgEnum {
  val IsTrue = Value(0)
  val NumericComparison = Value(1)
  val HasItemsInInventory = Value(2)
  val HasCharacterInParty = Value(3)

  def default = IsTrue
}

case class Condition(
  conditionTypeId: Int,
  var negate: Boolean,
  var operatorId: Int,
  var intValue1: IntParameter,
  var intValue2: IntParameter)