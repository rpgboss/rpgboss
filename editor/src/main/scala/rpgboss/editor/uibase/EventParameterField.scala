package rpgboss.editor.uibase

import scala.swing.Component
import scala.swing.Window
import rpgboss.editor.uibase.SwingUtils.indexedCombo
import rpgboss.editor.uibase.SwingUtils.percentField
import rpgboss.lib.Utils
import rpgboss.model.HasName
import rpgboss.model.PictureSlots
import rpgboss.model.ProjectData
import rpgboss.model.event._

/**
 * The name of the field and a component for editing the constant value.
 */
abstract class EventParameterField[T](
    val name: String, val model: EventParameter[T])
    (implicit m: reflect.Manifest[T]) {
  def constantComponentFactory(p: EventParameter[T]): Component

  def getModelComponent() = constantComponentFactory(model)

  def getModelCopyComponent() = {
    val modelCopy = Utils.deepCopy(model)
    val component = constantComponentFactory(modelCopy)
    (modelCopy, component)
  }
}

object EventParameterField {
  def IntNumberField(name: String, min: Int, max: Int, model: IntParameter) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        new NumberSpinner(p.constant, min, max, p.constant = _)
    }

  def IntEnumIdField[T <: HasName]
      (name: String, choices: Array[T], model: IntParameter) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        indexedCombo(choices, p.constant, p.constant = _)
    }

  def FloatPercentField(
      name: String, min: Float, max: Float,
      model: FloatParameter) =
    new EventParameterField[Float](name, model) {
      override def constantComponentFactory(p: EventParameter[Float]) =
        percentField(min, max, p.constant, p.constant =_)
    }

  def IntMultiselectField[T <: HasName](
      owner: Window,
      name: String,
      choices: Array[T],
      model: IntArrayParameter) =
    new EventParameterField[Array[Int]](name, model) {
      override def constantComponentFactory(p: EventParameter[Array[Int]]) =
        new ArrayMultiselectPanel(owner, name, choices, p.constant,
            p.constant = _)
    }

  def getParameterFields(
      owner: Window, pData: ProjectData, cmd: EventCmd):
      Seq[EventParameterField[_]] = cmd match {
    case c: AddRemoveItem => List(
        IntEnumIdField("Item", pData.enums.items, c.itemId),
        IntNumberField("Quantity", 1, 99, c.quantity))
    case c: AddRemoveGold => List(
        IntNumberField("Quantity", 1, 9999, c.quantity))
    case c: HidePicture => List(
        IntNumberField("Slot", PictureSlots.ABOVE_MAP,
            PictureSlots.BATTLE_BEGIN - 1, c.slot))
    case c: OpenStore => List(
        IntMultiselectField(
            owner, "Items Sold", pData.enums.items, c.itemIdsSold),
        FloatPercentField(
            "Buy price multiplier:", 0f, 4f, c.buyPriceMultiplier),
        FloatPercentField(
            "Sell price multiplier:", 0f, 4f, c.sellPriceMultiplier))
    case c: SetGlobalInt => List(
        IntNumberField("Value 1", -9999, 9999, c.value1),
        IntNumberField("Value 2", -9999, 9999, c.value2))
    case c: ShowPicture => List(
        IntNumberField("Slot", PictureSlots.ABOVE_MAP,
            PictureSlots.BATTLE_BEGIN - 1, c.slot))
    case _ => Nil
  }
}