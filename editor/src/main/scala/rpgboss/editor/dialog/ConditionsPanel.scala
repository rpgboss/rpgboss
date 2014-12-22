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
import rpgboss.editor.uibase.InlineWidgetArrayEditor
import rpgboss.editor.uibase.RpgPopupMenu
import scala.swing.MenuItem

class ConditionsPanel(
  owner: Window,
  pData: ProjectData,
  initial: Array[Condition],
  onUpdate: Array[Condition] => Unit)
  extends InlineWidgetArrayEditor(owner, initial, onUpdate) {

  override def title: String = needsTranslation("Event Conditions")
  override def addAction(index: Int) = {
    openEnumSelectDialog(ConditionType)(
        owner,
        needsTranslation("Condition Type"),
        conditionType => {
          insertElement(index, Condition.defaultInstance(conditionType))
        })
  }
  def newInlineWidget(element: Condition) = {
    import ConditionType._

    def intField(parameter: IntParameter) = {
      new ParameterFullComponent(
          owner,
          EventParameterField.IntNumberField(
              "", -9999, 9999, parameter, Some(sendUpdate)))
    }

    new BoxPanel(Orientation.Horizontal) {
      contents += new BoxPanel(Orientation.Vertical) {
        contents += boolField("NOT", element.negate, element.negate = _)

        contents += new BoxPanel(Orientation.Vertical) {
          ConditionType(element.conditionTypeId) match {
            case IsTrue =>
              contents += intField(element.intValue1)
              contents += lbl(" " + needsTranslation("is TRUE."))
            case NumericComparison =>
              contents += intField(element.intValue1)
              contents += lbl(" " + needsTranslation("is") + " ")
              contents += enumIdCombo(ComparisonOperator)(
                  element.operatorId, element.operatorId = _,
                  additionalAction = Some(sendUpdate),
                  customRenderer = Some(_.jsOperator))
              contents += intField(element.intValue2)
            case HasItemsInInventory =>
              contents += lbl(needsTranslation("Player has"))
              contents += intField(element.intValue2)
              contents += lbl(needsTranslation("or more"))
              contents += new ParameterFullComponent(
                  owner, EventParameterField.IntEnumIdField(
                      "", pData.enums.items, element.intValue1,
                       Some(sendUpdate)))
            case HasCharacterInParty =>
              contents += new ParameterFullComponent(
                  owner, EventParameterField.IntEnumIdField(
                      "", pData.enums.characters, element.intValue1,
                       Some(sendUpdate)))
              contents += lbl(needsTranslation(" is in the party."))
          }
        }
      }
    }
  }
}