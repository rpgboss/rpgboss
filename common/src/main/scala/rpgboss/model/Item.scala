package rpgboss.model

object ItemScope extends RpgEnum {
  val None = Value
  val SelfOnly = Value("User only")
  val OneEnemy = Value("One enemy")
  val AllEnemies = Value("All enemies")
  val OneAlly = Value("One ally")
  val AllAllies = Value("All allies")
  val OneAllyDead = Value("One dead ally")
  val AllAlliesDead = Value("All dead allies")

  def default = OneAlly
}

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

case class Item(
  name: String = "",
  desc: String = "",
  effects: Seq[Effect] = Seq(),

  sellable: Boolean = true,
  price: Int = 100,

  itemType: Int = ItemType.default.id,

  equipType: Int = 0,
  
  accessId: Int = ItemAccessibility.default.id,
  scopeId: Int = ItemScope.default.id,

  icon: Option[IconSpec] = None) extends HasName