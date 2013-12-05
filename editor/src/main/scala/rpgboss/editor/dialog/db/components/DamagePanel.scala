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
class DamagePanel(dbDiag: DatabaseDialog, model: Damage) 
  extends DesignGridPanel {
  
  val fElement = indexedComboStrings(
      dbDiag.model.enums.elements, 
      model.elementId,
      model.elementId = _)
      
  val fFormula = textField(model.formula, model.formula = _)
  
  row().grid(lbl("Element:")).add(fElement)
  row().grid(lbl("Formula:")).add(fFormula)
}

/**
 * @param onUpdate      Called when the array reference is updated. Array
 *                      may be updated in-place. onUpdate is not called then.
 */
class DamagesPanel(dbDiag: DatabaseDialog, initial: Seq[Damage],
                   onUpdate: Seq[Damage] => Unit)
  extends BoxPanel(Orientation.Vertical) {

  var model = initial
  
  val buttonPanel = new BoxPanel(Orientation.Horizontal) {
    contents += new Button(Action("Add") {
      model = model :+ Damage()
      damagesPanel.contents += new DamagePanel(dbDiag, model.last)
      damagesPanel.revalidate()
      onUpdate(model)
    })
    
    contents += new Button(Action("Remove Last") {
      if (model.length > 1) {
        model = model.dropRight(1)
        damagesPanel.contents.trimEnd(1)
        damagesPanel.revalidate()
        onUpdate(model)
      }
    })
  }
  
  val damagesPanel = new BoxPanel(Orientation.Vertical)
  model.foreach(v => damagesPanel.contents += new DamagePanel(dbDiag, v))
  
  contents += buttonPanel
  contents += new ScrollPane {
    contents = damagesPanel
  }
}