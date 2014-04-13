package rpgboss.model

object DamageType extends RpgEnum {
  val Physical, Magic = Value
  def default = Physical
}

case class Damage(
  var typeId: Int = DamageType.Physical.id,
  var elementId: Int = 0,
  var formula: String = "") {
  
}
  
case class Skill(
  var name: String = "",
  var scopeId: Int = Scope.OneEnemy.id,
  var cost: Int = 0,
  var damages: Seq[Damage] = Seq(Damage()),
  var effects: Seq[Effect] = Seq()) extends HasName