package rpgboss.model

case class Damage(
  var elementId: Int = 0,
  var formula: String = "10")

case class Skill(
  var name: String = "Skill",
  var scopeId: Int = Scope.OneEnemy.id,
  var damages: Seq[Damage] = Seq(Damage())) extends HasName