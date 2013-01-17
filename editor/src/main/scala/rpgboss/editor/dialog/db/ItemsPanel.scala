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
  def panelName = "Items/Equipment"
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
        
        val fSellable: CheckBox = 
          boolField(model.sellable, v => {
            updateModel(model.copy(sellable = v))
            setEnabledFields()
          })
        
        val fPrice = new NumberSpinner(
            model.price, 
            MINPRICE, 
            MAXPRICE,
            onUpdate = v => updateModel(model.copy(price = v)))
        
        val fItemType = enumCombo(ItemType)(
            model.itemType,
            v => {
              updateModel(model.copy(itemType = v.id))
              setEnabledFields()
            })
        
        val fEquipSlot = enumCombo(EquipSlot)(
            model.slot,
            v => updateModel(model.copy(slot = v.id)))
        
        val fScope = enumCombo(Scope)(
            model.scopeId,
            v => updateModel(model.copy(scopeId = v.id)))
            
        val fAccess = enumCombo(ItemAccessibility)(
            model.accessId,
            v => updateModel(model.copy(accessId = v.id)))
        
        def setEnabledFields() = {
          fPrice.enabled = fSellable.selected
          
          fEquipSlot.enabled = model.itemType == ItemType.Equipment.id
          fScope.enabled     = model.itemType != ItemType.Equipment.id
          fAccess.enabled    = model.itemType != ItemType.Equipment.id
        }
        
        setEnabledFields()
            
        row().grid(lbl("Name:")).add(fName)
        row().grid(lbl("Description:")).add(fDesc)
        row()
          .grid(lbl("Sellable:")).add(fSellable)
          .grid(lbl("Price:")).add(fPrice)
        
        row()
          .grid(lbl("Item type:")).add(fItemType)
          .grid(lbl("Equip slot:")).add(fEquipSlot)
        
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