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

  def editPaneForItem(idx: Int, model: Item) = {
    new BoxPanel(Orientation.Horizontal) with DisposableComponent {
      def effectContext =
        if (model.itemType == ItemType.Equipment.id)
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
          model.price,
          MINPRICE,
          MAXPRICE,
          onUpdate = model.price = _)

        val fItemType = enumIdCombo(ItemType)(
          model.itemType,
          model.itemType = _,
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
          "Use on Attack",
          model.useOnAttack,
          model.useOnAttack = _,
          Some(setEnabledFields _))

        val fOnUseSkillId = indexedCombo(
          dbDiag.model.enums.skills,
          model.onUseSkillId,
          model.onUseSkillId = _)

        def setEnabledFields(): Unit = {
          fPrice.enabled = fSellable.selected

          fScope.enabled = model.itemType != ItemType.Equipment.id
          fAccess.enabled = model.itemType != ItemType.Equipment.id

          fEquipType.enabled = model.itemType == ItemType.Equipment.id

          fUseOnAttack.enabled = model.itemType == ItemType.Equipment.id
          fOnUseSkillId.enabled =
            model.itemType == ItemType.Equipment.id && model.useOnAttack

          effectPane.updateContext(effectContext)
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

      contents += leftPane
      contents += effectPane
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.items = dataAsArray
  }
}