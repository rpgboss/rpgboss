package rpgboss.editor.dialog

import scala.swing.BoxPanel
import scala.swing.Orientation
import scala.swing.Window
import rpgboss.model.event.Condition
import rpgboss.editor.Internationalized._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.ScrollPane
import javax.swing.BorderFactory
import rpgboss.model.event.ConditionType
import rpgboss.editor.uibase.ParameterFullComponent
import rpgboss.editor.uibase.EventParameterField
import rpgboss.model.event.IntParameter
import rpgboss.model.event.ComparisonOperator
import rpgboss.model.ProjectData

class ConditionsPanel(
  owner: Window,
  initial: Array[Condition],
  onOk: Array[Condition] => Unit,
  pData: ProjectData)
  extends BoxPanel(Orientation.Vertical) {

  border = BorderFactory.createTitledBorder(getMessage("Conditions"))

  val arrayContainer = new BoxPanel(Orientation.Vertical) {

  }

  def paneForElement(index: Int, model: Condition) = {
    import ConditionType._

    def intField(parameter: IntParameter) = {
      new ParameterFullComponent(
          owner, EventParameterField.IntNumberField("", -9999, 9999, parameter))
    }

    new BoxPanel(Orientation.Vertical) {
      contents += boolField("NOT", model.negate, model.negate = _)

      contents += new BoxPanel(Orientation.Horizontal) {
        ConditionType(model.conditionTypeId) match {
          case IsTrue =>
            contents += intField(model.intValue1)
            contents += lbl(needsTranslation("is TRUE."))
          case NumericComparison =>
            contents += intField(model.intValue1)
            contents += enumIdCombo(ComparisonOperator)(
                model.operatorId, model.operatorId = _)
            contents += intField(model.intValue2)
          case HasItemsInInventory =>
            contents += lbl(needsTranslation("Inventory contains "))
            contents += intField(model.intValue2)
            contents += lbl(needsTranslation(" or more of "))
            contents += new ParameterFullComponent(
                owner, EventParameterField.IntEnumIdField(
                    "", pData.enums.items, model.intValue1))
          case HasCharacterInParty =>
            contents += new ParameterFullComponent(
                owner, EventParameterField.IntEnumIdField(
                    "", pData.enums.characters, model.intValue1))
            contents += lbl(needsTranslation("is in the party."))
        }
      }
    }
  }

  contents += new ScrollPane(arrayContainer)
}