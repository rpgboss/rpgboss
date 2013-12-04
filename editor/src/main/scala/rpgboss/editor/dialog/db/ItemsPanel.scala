package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
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
  extends RightPaneArrayDatabasePanel(
    owner, 
    "Items", 
    dbDiag.model.enums.items)
  with DatabasePanel {
  def panelName = "Items/Equipment"
  def newDefaultInstance() = new Item()
  def label(item: Item) = item.name

  def editPaneForItem(idx: Int, model: Item) = {
    new BoxPanel(Orientation.Horizontal) {
      val leftPane = new DesignGridPanel {
        val fName = textField(model.name, v => {
          model.name = v
          refreshModel()
        })

        val fDesc = textField(model.desc, model.desc = _)

        val fSellable: CheckBox = boolField(model.sellable, v => {
          model.sellable = v
          setEnabledFields()
        })

        val fPrice = new NumberSpinner(
          model.price,
          MINPRICE,
          MAXPRICE,
          onUpdate = model.price = _)

        val fItemType = enumCombo(ItemType)(
          model.itemType,
          v => {
            model.itemType = v.id
            setEnabledFields()
          })

        val fScope = enumCombo(Scope)(
          model.scopeId,
          v => model.scopeId = v.id)

        val fAccess = enumCombo(ItemAccessibility)(
          model.accessId,
          v => model.accessId = v.id)

        val fEquipType = indexedComboStrings(
          dbDiag.model.enums.equipTypes,
          model.equipType,
          model.equipType = _)
          
        val fUseOnAttack = boolField(
          model.useOnAttack, 
          model.useOnAttack = _, 
          "Use on Attack")
          
        val fOnUseSkillId = indexedCombo(
          dbDiag.model.enums.skills,
          model.onUseSkillId,
          model.onUseSkillId = _)
          
        def setEnabledFields() = {
          fPrice.enabled = fSellable.selected

          fScope.enabled = model.itemType != ItemType.Equipment.id
          fAccess.enabled = model.itemType != ItemType.Equipment.id
          
          fEquipType.enabled = model.itemType == ItemType.Equipment.id
          
          fUseOnAttack.enabled = model.itemType == ItemType.Equipment.id
          fOnUseSkillId.enabled =
            model.itemType == ItemType.Equipment.id && model.useOnAttack
        }

        setEnabledFields()

        row().grid(lbl("Name:")).add(fName)
        row().grid(lbl("Description:")).add(fDesc)
        row()
          .grid(lbl("Sellable:")).add(fSellable)
          .grid(lbl("Price:")).add(fPrice)

        row()
          .grid(lbl("Item type:")).add(fItemType)

        row()
          .grid(lbl("Effect scope:")).add(fScope)
          .grid(lbl("Item access:")).add(fAccess)
        
        row()
          .grid(lbl("Equip type:")).add(fEquipType)
        row().grid().add(fUseOnAttack)
        row()
          .grid(lbl("On use skill:")).add(fOnUseSkillId)
      }

      val rightPane = 
        new EffectPanel(owner, dbDiag, model.effects, model.effects = _)

      contents += leftPane
      contents += rightPane
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.items = arrayBuffer
  }
}