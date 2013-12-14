package rpgboss.model

object DamageType extends RpgEnum {
  val Physical, Magic = Value
  def default = Physical
}

case class Damage(
  var typeId: Int = DamageType.Physical.id,
  var elementId: Int = 0,
  var formula: String = "10")

case class Skill(
  var name: String = "Skill",
  var scopeId: Int = Scope.OneEnemy.id,
  var damages: Seq[Damage] = Seq(Damage())) extends HasName