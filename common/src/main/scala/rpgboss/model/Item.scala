package rpgboss.model

object ItemType extends RpgEnum {
  val Consumable, Reusable, Equipment = Value
  val KeyItem = Value("Key item")

  def default = Consumable
}

object ItemAccessibility extends RpgEnum {
  val Always = Value
  val MenuOnly = Value("Menu only")
  val BattleOnly = Value("Battle only")
  val Never = Value

  def default = Always
}

object Item {
}

/**
 * @param scopeId       Affected targets for consumables
 */
case class Item(
  var name: String = "Item",
  var desc: String = "Item description",
  var effects: Seq[Effect] = Seq(),

  var sellable: Boolean = true,
  var price: Int = 100,

  var itemType: Int = ItemType.default.id,

  var accessId: Int = ItemAccessibility.default.id,
  var scopeId: Int = Scope.default.id,
  
  var equipType: Int = 0,
  
  var onUseSkillId: Int = 0,
  
  var icon: Option[IconSpec] = None) extends HasName