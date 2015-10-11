package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.dialog._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.Internationalized._

class ItemsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.items)
  with DatabasePanel {
  def panelName = getMessage("Items_Equipment")
  def newDefaultInstance() = new Item()

  def editPaneForItem(idx: Int, model: Item) = {
    new BoxPanel(Orientation.Horizontal) with DisposableComponent {
      def effectContext =
        if (model.itemTypeId == ItemType.Equipment.id)
          EffectContext.Equipment
        else
          EffectContext.Item

      val effectPane =
        new EffectPanel(owner, dbDiag, model.effects, model.effects = _,
          effectContext)

      val leftPane = new DesignGridPanel {
        import HasName._

        val fName = textField(model.name, v => {
          model.name = v
          refreshModel()
        })

        val fDesc = textField(model.desc, model.desc = _)

        val fSellable: CheckBox = boolField(
          "",
          model.sellable,
          model.sellable = _,
          Some(setEnabledFields _))

        val fPrice = new NumberSpinner(
          MINPRICE,
          MAXPRICE,
          model.price,
          onUpdate = model.price = _)

        val fItemType = enumIdCombo(ItemType)(
          model.itemTypeId,
          model.itemTypeId = _,
          Some(setEnabledFields _))

        val fScope = enumIdCombo(Scope)(model.scopeId, model.scopeId = _)

        val fAccess = enumIdCombo(ItemAccessibility)(
          model.accessId,
          model.accessId = _)

        val fEquipType = indexedCombo(
          dbDiag.model.enums.equipTypes,
          model.equipType,
          model.equipType = _,
          Some(setEnabledFields _))

        val fUseOnAttack = boolField(
          getMessage("Use_On_Attack"),
          model.useOnAttack,
          model.useOnAttack = _,
          Some(setEnabledFields _))

        val fEquippedAttackSkillId = indexedCombo(
          dbDiag.model.enums.skills,
          model.equippedAttackSkillId,
          model.equippedAttackSkillId = _)

        def setEnabledFields(): Unit = {
          fPrice.enabled = fSellable.selected

          fScope.enabled = model.itemTypeId != ItemType.Equipment.id
          fAccess.enabled = model.itemTypeId != ItemType.Equipment.id

          fEquipType.enabled = model.itemTypeId == ItemType.Equipment.id

          fUseOnAttack.enabled = model.itemTypeId == ItemType.Equipment.id
          fEquippedAttackSkillId.enabled =
            model.itemTypeId == ItemType.Equipment.id && model.useOnAttack

          effectPane.updateContext(effectContext)
        }

        setEnabledFields()

        row().grid(lbl(getMessageColon("Name"))).add(fName)
        row().grid(lbl(getMessageColon("Description"))).add(fDesc)
        row()
          .grid(lbl(getMessageColon("Sellable"))).add(fSellable)
        row()
          .grid(lbl(getMessageColon("Price"))).add(fPrice)

        row()
          .grid(lbl(getMessageColon("Item_Type"))).add(fItemType)

        row()
          .grid(lbl(getMessageColon("Effect_Scope"))).add(fScope)
        row()
          .grid(lbl(getMessageColon("Item_Access"))).add(fAccess)

        row()
          .grid(lbl(getMessageColon("Equip_Type"))).add(fEquipType)
        row().grid().add(fUseOnAttack)
        row()
          .grid(lbl(getMessageColon("Attack_Skill"))).add(fEquippedAttackSkillId)
      }

      contents += leftPane
      contents += effectPane
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.items = dataAsArray
  }
}