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
import rpgboss.editor.Internationalized._
import rpgboss.player.RpgScreen

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
  def IntNumberField(
      name: String, min: Int, max: Int, model: IntParameter,
      additionalAction: Option[() => Unit] = None) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        new NumberSpinner(
            min, max, p.constant, p.constant = _, additionalAction)
    }

  def IntEnumIdField[T <: HasName](
      name: String, choices: Array[T], model: IntParameter,
      additionalAction: Option[() => Unit] = None) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        indexedCombo(choices, p.constant, p.constant = _, additionalAction)
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
        IntEnumIdField(getMessage("Item"), pData.enums.items, c.itemId),
        IntNumberField(getMessage("Quantity"), 1, 99, c.quantity))
    case c: AddRemoveGold => List(
        IntNumberField(getMessage("Quantity"), 1, 9999, c.quantity))
    case c: HidePicture => List(
        IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
            PictureSlots.BATTLE_BEGIN - 1, c.slot))
    case c: OpenStore => List(
        IntMultiselectField(
            owner, getMessage("Items_Sold"), pData.enums.items, c.itemIdsSold),
        FloatPercentField(
            getMessageColon("Buy_Price_Multiplier"), 0f, 4f, c.buyPriceMultiplier),
        FloatPercentField(
            getMessageColon("Sell_Price_Multiplier"), 0f, 4f, c.sellPriceMultiplier))
    case c: PlayMusic => List(
        IntNumberField(getMessage("Slot"), 0, RpgScreen.MAX_MUSIC_SLOTS,
            c.slot))
    case c: SetGlobalInt => List(
        IntNumberField(getMessage("Value") + " 1", -9999, 9999, c.value1),
        IntNumberField(getMessage("Value") + " 2", -9999, 9999, c.value2))
    case c: ShowPicture => List(
        IntNumberField(getMessage("Slot"), PictureSlots.ABOVE_MAP,
            PictureSlots.BATTLE_BEGIN - 1, c.slot))
    case c: StopMusic => List(
        IntNumberField(getMessage("Slot"), 0, RpgScreen.MAX_MUSIC_SLOTS,
            c.slot))
    case _ => Nil
  }
}