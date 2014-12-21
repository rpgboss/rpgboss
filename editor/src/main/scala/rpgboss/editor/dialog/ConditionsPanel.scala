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
import scala.swing.Button
import scala.swing.Action
import scala.collection.mutable.ArrayBuffer

class ConditionsPanel(
  owner: Window,
  model: ArrayBuffer[Condition],
  pData: ProjectData)
  extends BoxPanel(Orientation.Vertical) {

  border = BorderFactory.createTitledBorder(getMessage("Conditions"))

  val arrayContainer = new BoxPanel(Orientation.Vertical) {

  }

  def paneForElement(index: Int, element: Condition) = {
    import ConditionType._

    def intField(parameter: IntParameter) = {
      new ParameterFullComponent(
          owner, EventParameterField.IntNumberField("", -9999, 9999, parameter))
    }

    new BoxPanel(Orientation.Horizontal) {
      contents += new BoxPanel(Orientation.Vertical) {
        contents += boolField("NOT", element.negate, element.negate = _)

        contents += new BoxPanel(Orientation.Horizontal) {
          ConditionType(element.conditionTypeId) match {
            case IsTrue =>
              contents += intField(element.intValue1)
              contents += lbl(needsTranslation("is TRUE."))
            case NumericComparison =>
              contents += intField(element.intValue1)
              contents += enumIdCombo(ComparisonOperator)(
                  element.operatorId, element.operatorId = _)
              contents += intField(element.intValue2)
            case HasItemsInInventory =>
              contents += lbl(needsTranslation("Inventory contains "))
              contents += intField(element.intValue2)
              contents += lbl(needsTranslation(" or more of "))
              contents += new ParameterFullComponent(
                  owner, EventParameterField.IntEnumIdField(
                      "", pData.enums.items, element.intValue1))
            case HasCharacterInParty =>
              contents += new ParameterFullComponent(
                  owner, EventParameterField.IntEnumIdField(
                      "", pData.enums.characters, element.intValue1))
              contents += lbl(needsTranslation("is in the party."))
          }
        }
      }

      contents += new Button(Action(needsTranslation("Delete")) {
      })
    }
  }

  contents += new ScrollPane(arrayContainer)
}