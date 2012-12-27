package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.editor.dialog._

import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class ItemsPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends RightPaneArrayEditingPanel(owner, "Items", dbDiag.model.items)
  with DatabasePanel
{
  def panelName = "Items"
  def newDefaultInstance() = new Item()
  def label(item: Item) = item.name
  
  def editPaneForItem(idx: Int, initial: Item) = {
    var model = initial
      
    def updateModel(newModel: Item) = {
      model = newModel
      updatePreserveSelection(idx, model)
    }

    new BoxPanel(Orientation.Horizontal) {
      val leftPane = new DesignGridPanel {
        val fName = 
          textField(model.name, v => updateModel(model.copy(name = v))) 
          
        val fDesc =
          textField(model.desc, v => updateModel(model.copy(desc = v)))
        
        val fPrice = new NumberSpinner(
            model.price, 
            MINPRICE, 
            MAXPRICE,
            onUpdate = v => updateModel(model.copy(price = v)))
        
        val fItemType = enumCombo(ItemType)(
            model.itemType,
            v => updateModel(model.copy(itemType = v.id)),
            Array(ItemType.Consumable, ItemType.Rare)) 
        
        val fScope = enumCombo(Scope)(
            model.scopeId,
            v => updateModel(model.copy(scopeId = v.id)))
            
        val fAccess = enumCombo(ItemAccessibility)(
            model.accessId,
            v => updateModel(model.copy(accessId = v.id)))
        
        row().grid(lbl("Name:")).add(fName)
        row().grid(lbl("Description:")).add(fDesc)
        row()
          .grid(lbl("Price:")).add(fPrice)
          .grid(lbl("Item type:")).add(fItemType)
        
        row()
          .grid(lbl("Effect scope:")).add(fScope)
          .grid(lbl("Item access:")).add(fAccess)
      }
      
      val rightPane = new EffectPanel(owner, dbDiag, model.effects, es => {
        updateModel(model.copy(effects = es))
      })
      
      contents += leftPane
      contents += rightPane
    }
  }
  
  override def onListDataUpdate() = {
    logger.info("Items data updated")
    dbDiag.model = dbDiag.model.copy(
        items = array
    )
  }
}