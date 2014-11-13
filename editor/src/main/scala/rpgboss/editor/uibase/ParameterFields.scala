package rpgboss.editor.uibase

import SwingUtils._
import scala.swing._
import rpgboss.model.event.IntParameter
import rpgboss.model.event.EventParameterValueType
import rpgboss.model.HasName

class IntParameterField(
    model: IntParameter,
    fConstant: Component) extends GridPanel(2, 2) {
  val fParameter = textField(model.parameter, model.parameter = _)

  def updateFields() = {
    fConstant.enabled = model.valueTypeId == EventParameterValueType.Constant.id
    fParameter.enabled =
      model.valueTypeId == EventParameterValueType.Parameter.id
  }
  updateFields()

  val valueTypeBtns = enumIdRadios(EventParameterValueType)(
      model.valueTypeId,
      v => {
        model.valueTypeId = v
        updateFields()
      })
  val group = makeButtonGroup(valueTypeBtns)

  contents += valueTypeBtns(0)
  contents += fConstant

  contents += valueTypeBtns(1)
  contents += fParameter
}

class IntParameterNumberField(
    model: IntParameter,
    min: Int,
    max: Int)
    extends IntParameterField(
        model,
        new NumberSpinner(model.constant, min, max, model.constant = _))

class IntParameterEnumerationIndexField[T <% HasName](
    model: IntParameter,
    choices: Seq[T]) extends IntParameterField(
        model,
        indexedCombo(choices, model.constant, model.constant = _))