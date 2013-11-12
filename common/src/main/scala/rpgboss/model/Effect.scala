package rpgboss.model

object EffectKey extends RpgEnum {
  case class Val(desc: String) extends super.Val

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val RecoverHpAdd = Val("Recover HP")
  val RecoverHpMul = Val("Recover percentage of HP")
  val RecoverMpAdd = Val("Recover MP")
  val RecoverMpMul = Val("Recover percentage of MP")

  val AddStatusEffect = Val("Add status effect")
  val RemoveStatusEffect = Val("Remove status effect")

  val MhpAdd = Val("Increase Max HP")
  val MmpAdd = Val("Increase Max MP")
  val AtkAdd = Val("Increase ATK")
  val AgiAdd = Val("Increase AGI")
  val MagAdd = Val("Increase MAG")
  
  val EscapeBattle = Val("Escape battle")

  val UseSkill = Val("Use skill")
  val LearnSkill = Val("Learn skill")

  def default = RecoverHpAdd

  def defaultEffect = Effect(RecoverHpAdd.toString, 0)
}