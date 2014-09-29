package rpgboss.editor.dialog.db.components

import rpgboss.model._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import rpgboss.editor.uibase.DesignGridPanel
import javax.swing.BorderFactory
import rpgboss.editor.dialog.DatabaseDialog

/**
 * Updates model in-place.
 */
class DamageFormulaPanel(
  dbDiag: DatabaseDialog,
  initial: DamageFormula,
  onUpdate: () => Unit)
  extends DesignGridPanel {
  val model = initial

  val fElement = indexedCombo(
    dbDiag.model.enums.elements,
    model.elementId,
    model.elementId = _,
    Some(onUpdate))

  val fFormula = textField(model.formula, model.formula = _, Some(onUpdate))

  val radiosType = enumIdRadios(DamageType)(model.typeId, v => {
    model.typeId = v
    onUpdate()
  })

  val panelType = new BoxPanel(Orientation.Horizontal)
  addBtnsAsGrp(panelType.contents, radiosType)

  row().grid(lbl("Damage type:")).add(panelType)
  row().grid(lbl("Element:")).add(fElement)
  row().grid(lbl("Formula:")).add(fFormula)
}

/**
 * @param onUpdate      Called when the array reference is updated. Array
 *                      may be updated in-place. onUpdate is not called then.
 */
class DamageFormulaArrayPanel(
    dbDiag: DatabaseDialog,
    initial: Array[DamageFormula],
    onUpdate: Array[DamageFormula] => Unit)
  extends BoxPanel(Orientation.Vertical) {

  minimumSize = new Dimension(0, 500)

  var model = initial

  val buttonPanel = new BoxPanel(Orientation.Horizontal) {
    contents += new Button(Action("Add") {
      model = model :+ DamageFormula()
      damagesPanel.contents +=
        new DamageFormulaPanel(dbDiag, model.last, () => onUpdate(model))
      damagesPanel.revalidate()
      onUpdate(model)
    })

    contents += new Button(Action("Remove Last") {
      if (model.length > 0) {
        model = model.dropRight(1)
        damagesPanel.contents.trimEnd(1)
        damagesPanel.revalidate()
        damagesPanel.repaint()
        onUpdate(model)
      }
    })
  }

  val damagesPanel = new BoxPanel(Orientation.Vertical)
  model.foreach(v =>
    damagesPanel.contents += new DamageFormulaPanel(dbDiag, v, () => onUpdate(model)))

  border = BorderFactory.createTitledBorder("Damage")
  contents += buttonPanel
  contents += new ScrollPane {
    contents = damagesPanel
  }
}