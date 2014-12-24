package rpgboss.model.event

import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.RpgEnum
import rpgboss.player.PersistentState
import rpgboss.player.PersistentState
import rpgboss.player.HasScriptConstants
import rpgboss.player.ScriptInterface
import com.typesafe.scalalogging.slf4j.LazyLogging

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
  var intValue1: IntParameter = IntParameter(),
  var intValue2: IntParameter = IntParameter(),
  var operatorId: Int = OperatorType.default.id,
  var negate: Boolean = false) {
  def rawJs = {
    import ConditionType._
    import EventJavascript._
    ConditionType(conditionTypeId) match {
      case IsTrue => intValue1.rawJs
      case NumericComparison =>
        applyOperator(
            intValue1.rawJs,
            ComparisonOperator(operatorId).jsOperator,
            intValue2.rawJs)
      case HasItemsInInventory =>
        applyOperator(
            jsCall("game.countItems", intValue1),
            ComparisonOperator.GE.jsOperator,
            intValue2.rawJs)
      case HasCharacterInParty =>
        RawJs("""game.getIntArray(game.PARTY()).indexOf(%s) != -1""".format(
            intValue1.rawJs.exp))
    }
  }
}

object Condition extends LazyLogging {
  def defaultInstance(conditionType: ConditionType.Value) = {
    import ConditionType._
    conditionType match {
      case IsTrue => Condition(IsTrue.id, IntParameter.globalVariable())
      case NumericComparison =>
        Condition(
            NumericComparison.id,
            IntParameter.globalVariable(),
            IntParameter(1),
            operatorId = ComparisonOperator.GE.id)
      case HasItemsInInventory =>
        Condition(
            HasItemsInInventory.id,
            IntParameter(),
            IntParameter(1))
      case HasCharacterInParty =>
        Condition(
            HasCharacterInParty.id,
            IntParameter())
    }
  }

  def allConditionsTrue(
      conditions: Array[Condition],
      scriptInterface: ScriptInterface): Boolean = {
    val jsContext = Context.enter()
    val jsScope = jsContext.initStandardObjects()

    ScriptableObject.putProperty(
      jsScope, "game", Context.javaToJS(scriptInterface, jsScope))
//    try {
      for (condition <- conditions) {
        val conditionExp = condition.rawJs.exp
        val jsResult = jsContext.evaluateString(
          jsScope,
          conditionExp,
          "Condition: %s".format(conditionExp),
          1,
          null)

        val result = Context.toBoolean(jsResult)
        if (!result)
          return false
      }

      Context.exit()
//    } catch {
//      case e: Throwable => {
//        logger.error(
//          "Error while calculating conditions: %s. ".format(conditions.deep) +
//          "Error: %s".format(e.getMessage()))
//        return false
//      }
//    }

    return true
  }
}