package rpgboss.model.event

import rpgboss.MapScreenTest
import rpgboss.UnitSpec

class ConditionSpec extends UnitSpec {
  import ConditionType._
  import ComparisonOperator._

  val conditionBoolean = Condition(
      IsTrue.id,
      IntParameter.globalVariable("var1"))

  val conditionNumeric = Condition(
      NumericComparison.id,
      IntParameter.globalVariable("var1"),
      IntParameter(2),
      GE.id)

  val conditionItem = Condition(
      HasItemsInInventory.id,
      IntParameter(0),
      IntParameter(2))

  val conditionHasSecondCharacter = Condition(
      HasCharacterInParty.id,
      IntParameter(1))

  val allConditions =
    Array(conditionBoolean, conditionNumeric, conditionItem,
        conditionHasSecondCharacter)

  "Conditions" should "evaluate correctly" in {
    val test = new MapScreenTest {
      override def testScriptOnGdxThread() = {
        def testIndividual() = allConditions.map(
            c => Condition.allConditionsTrue(Array(c), scriptInterface))
        def testAll() =
          Condition.allConditionsTrue(allConditions, scriptInterface)

        testIndividual() should deepEqual(Array(false, false, false, false))

        persistent.setInt("var1", 1)
        testIndividual() should deepEqual(Array(true, false, false, false))
        testAll() should equal(false)

        persistent.setInt("var1", 2)
        testIndividual() should deepEqual(Array(true, true, false, false))
        testAll() should equal(false)

        persistent.addRemoveItem(0, 1)
        testIndividual() should deepEqual(Array(true, true, false, false))
        testAll() should equal(false)

        persistent.addRemoveItem(0, 1)
        testIndividual() should deepEqual(Array(true, true, true, false))
        testAll() should equal(false)

        persistent.modifyParty(true, 1)
        testIndividual() should deepEqual(Array(true, true, true, true))
        testAll() should equal(true)
      }
    }

    test.runTest()
  }

  "Conditions" should "render correctly in arrays" in {
    Condition.allConditionsExp(Array()) should deepEqual(RawJs("true"))
    Condition.allConditionsExp(Array(conditionNumeric)) should deepEqual(
        RawJs("game.getInt(\"var1\") >= 2"))
    Condition.allConditionsExp(Array(
        conditionNumeric, conditionItem, conditionBoolean)) should deepEqual(
        RawJs("(game.getInt(\"var1\") >= 2) && (game.countItems(0) >= 2) && (game.getInt(\"var1\"))"))
  }
}