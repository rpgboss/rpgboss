package rpgboss.editor.uibase

import scala.swing.Component
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model.HasName
import rpgboss.model.event.IntParameter
import rpgboss.model.event.EventCmd
import rpgboss.model.event.AddRemoveItem
import rpgboss.model.ProjectData

/**
 * The name of the field and a component for editing the constant value.
 */
case class EventParameterField[T](
    name: String, model: T, component: Component)

object EventParameterField {
  def IntNumberField(name: String, min: Int, max: Int, model: IntParameter) =
    EventParameterField[IntParameter](
        name,
        model,
        new NumberSpinner(model.constant, min, max, model.constant = _))

  def IntEnumIdField[T <% HasName]
      (name: String, choices: Array[T], model: IntParameter) =
    EventParameterField[IntParameter](
        name,
        model,
        indexedCombo(choices, model.constant, model.constant = _))

  def getConstantFields(pData: ProjectData, cmd: EventCmd) = cmd match {
    case c: AddRemoveItem => List(
        IntEnumIdField("Item", pData.enums.items, c.itemId),
        IntNumberField("Quantity", 1, 99, c.quantity))
    case _ => Nil
  }
}