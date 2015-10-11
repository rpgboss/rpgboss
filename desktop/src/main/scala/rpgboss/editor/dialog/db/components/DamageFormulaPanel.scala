package rpgboss.editor.dialog.db.components

import rpgboss.model._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import rpgboss.editor.uibase.DesignGridPanel
import javax.swing.BorderFactory
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.Internationalized._
import rpgboss.editor.uibase.InlineWidgetArrayEditor

/**
 * Updates model in-place.
 */
class DamageFormulaPanel(
  dbDiag: DatabaseDialog,
  initial: DamageFormula,
  onUpdate: () => Unit)
  extends DesignGridPanel {
  val model = initial

  val fType = enumIdCombo(DamageType)(model.typeId, v => {
    model.typeId = v
    onUpdate()
  })

  val fElement = indexedCombo(
    dbDiag.model.enums.elements,
    model.elementId,
    model.elementId = _,
    Some(onUpdate))

  val fFormula = textField(model.formula, model.formula = _, Some(onUpdate))

  row().grid(lbl(getMessageColon("Damage_Type"))).add(fType)
  row().grid(lbl(getMessageColon("Element"))).add(fElement)
  row().grid(lbl(getMessageColon("Formula"))).add(fFormula)
}

/**
 * @param onUpdate      Called when the array reference is updated. Array
 *                      may be updated in-place. onUpdate is not called then.
 */
class DamageFormulaArrayPanel(
    dbDiag: DatabaseDialog,
    initial: Array[DamageFormula],
    onUpdate: Array[DamageFormula] => Unit)
  extends InlineWidgetArrayEditor(dbDiag, initial, onUpdate) {
  override def title = getMessage("Damage")
  override def addAction(index: Int) = insertElement(index, DamageFormula())
  override def newInlineWidget(elementModel: DamageFormula) =
    new DamageFormulaPanel(dbDiag, elementModel, sendUpdate)
}