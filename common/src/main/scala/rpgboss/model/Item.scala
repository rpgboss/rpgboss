package rpgboss.model

object ItemType extends RpgEnum {
  val Consumable, Reusable, Equipment = Value
  val KeyItem = Value("Key_Item")

  def default = Consumable
}

object ItemAccessibility extends RpgEnum {
  val Always = Value
  val MenuOnly = Value("Menu_Only")
  val BattleOnly = Value("Battle_Only")
  val Never = Value

  def default = Always
}

object Item {
}

/**
 * @param scopeId                 Affected targets for consumables
 * @param equippedAttackSkillId   When this item is equipped, and user selects
 *                                "Attack" in battle, this skill is used.
 */
case class Item(
  var name: String = "",
  var desc: String = "",
  var effects: Array[Effect] = Array(),

  var sellable: Boolean = true,
  var price: Int = 100,

  var itemTypeId: Int = ItemType.default.id,

  var accessId: Int = ItemAccessibility.default.id,
  var scopeId: Int = Scope.default.id,

  var equipType: Int = 0,

  var useOnAttack: Boolean = false,
  var equippedAttackSkillId: Int = 0,

  var icon: Option[IconSpec] = None) extends HasName {

  def usableInMenu = usableIn(ItemAccessibility.MenuOnly)
  def usableInBattle = usableIn(ItemAccessibility.BattleOnly)

  private def usableIn(extraContext: ItemAccessibility.Value) = {
    import ItemType._
    import ItemAccessibility._

    val itemType = ItemType(itemTypeId)
    val access = ItemAccessibility(accessId)

    (itemType == Consumable || itemType == Reusable || itemType == KeyItem) &&
      (access == Always || access == extraContext)
  }
}