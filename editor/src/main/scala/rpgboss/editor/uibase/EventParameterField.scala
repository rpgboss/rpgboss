package rpgboss.editor.uibase

import scala.swing.Component
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model.HasName
import rpgboss.model.event.IntParameter
import rpgboss.model.event.EventCmd
import rpgboss.model.event.AddRemoveItem
import rpgboss.model.ProjectData
import rpgboss.model.event.AddRemoveGold

/**
 * The name of the field and a component for editing the constant value.
 */
case class EventParameterField[T](
    name: String, model: T, constantComponentFactory: T => Component)

object EventParameterField {
  def IntNumberField(name: String, min: Int, max: Int, model: IntParameter) =
    EventParameterField[IntParameter](
        name,
        model,
        p => new NumberSpinner(p.constant, min, max, p.constant = _))

  def IntEnumIdField[T <% HasName]
      (name: String, choices: Array[T], model: IntParameter) =
    EventParameterField[IntParameter](
        name,
        model,
        p => indexedCombo(choices, p.constant, p.constant = _))

  def getParameterFields(pData: ProjectData, cmd: EventCmd) = cmd match {
    case c: AddRemoveItem => List(
        IntEnumIdField("Item", pData.enums.items, c.itemId),
        IntNumberField("Quantity", 1, 99, c.quantity))
    case c: AddRemoveGold => List(
        IntNumberField("Quantity", 1, 9999, c.quantity))
    case _ => Nil
  }
}