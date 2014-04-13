package rpgboss.model

import rpgboss.lib._

case class Effect(keyId: Int, var v1: Int, var v2: Int)

object EffectKey extends RpgEnum {
  val defaultRenderer = (e: Effect, pData: ProjectData) => e.v1.toString

  case class Val(
    desc: String,
    renderer: (Effect, ProjectData) => String = defaultRenderer)
    extends super.Val

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]
  
  /**
   * Renders the value of the enum index stored in v1.
   */
  def getEnumOfValue1[T <% HasName](getChoices: ProjectData => Seq[T]) = 
    (e: Effect, pData: ProjectData) => {
      val choices = getChoices(pData)
      val skillName =
        if (e.v1 < pData.enums.skills.length)
          pData.enums.skills(e.v1).name
        else
          "<Past end of array>"
      StringUtils.standardIdxFormat(e.v1, skillName)
    }
    
  /**
   * Renders the value of the enum index stored in v1, and then shows the number
   * stored in v2.
   */
  def getEnumOfValue2[T <% HasName](getChoices: ProjectData => Seq[T]) = {
    val v1Func = getEnumOfValue1(getChoices)
    (e: Effect, pData: ProjectData) => {
      "%s. Value = %d ".format(v1Func(e, pData), e.v2)
    }
  }
    

  val RecoverHpAdd = Val("Recover HP")
  val RecoverHpMul = Val("Recover percentage of HP")
  val RecoverMpAdd = Val("Recover MP")
  val RecoverMpMul = Val("Recover percentage of MP")

  val AddStatusEffect = 
    Val("Add status effect", getEnumOfValue1(_.enums.statusEffects))
  val RemoveStatusEffect = 
    Val("Remove status effect", getEnumOfValue1(_.enums.statusEffects))

  val MhpAdd = Val("Increase Max HP")
  val MmpAdd = Val("Increase Max MP")
  val AtkAdd = Val("Increase Attack")
  val SpdAdd = Val("Increase Speed")
  val MagAdd = Val("Increase Magic")
  val ArmAdd = Val("Increase Armor")
  val MreAdd = Val("Increase Magic Resist")
  
  val ElementResist = Val("Resist Element", getEnumOfValue2(_.enums.elements))

  val EscapeBattle = Val("Escape battle")

  val UseSkill = Val("Use skill", getEnumOfValue1(_.enums.skills))
  val LearnSkill = Val("Learn skill", getEnumOfValue1(_.enums.skills))

  def default = RecoverHpAdd

  def defaultEffect = Effect(RecoverHpAdd.id, 0, 0)
}