package rpgboss.model

import rpgboss.lib._

/**
 * Because effects have different meanings in different contexts, we provide
 * a way to get the validity and meaning of an effect in this context.
 */
object EffectContext extends Enumeration {
  val CharacterClass, Item, Equipment, Skill, StatusEffect = Value

  case class EffectHelp(valid: Boolean, helpMessage: String)
}

case class Effect(keyId: Int, var v1: Int, var v2: Int)

object EffectKey extends RpgEnum {
  import EffectContext._

  val defaultRenderer = (e: Effect, pData: ProjectData) => e.v1.toString
  val defaultHelp = (context: EffectContext.Value) =>
    EffectHelp(false, "TODO: Implement a help message here.")

  case class Val(
    desc: String,
    renderer: (Effect, ProjectData) => String = defaultRenderer,
    help: (EffectContext.Value) => EffectHelp = defaultHelp)
    extends super.Val

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  /**
   * Renders the value of the enum index stored in v1.
   */
  def getEnumOfValue1[T <% HasName](getChoices: ProjectData => Array[T]) =
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
  def getEnumOfValue2[T <% HasName](getChoices: ProjectData => Array[T]) = {
    val v1Func = getEnumOfValue1(getChoices)
    (e: Effect, pData: ProjectData) => {
      "%s. Value = %d ".format(v1Func(e, pData), e.v2)
    }
  }

  def recoveryHelp(context: EffectContext.Value) = context match {
    case Item => EffectHelp(true, "One-time effect of item use.")
    case Skill => EffectHelp(true, "One-time effect of skill use.")
    case StatusEffect => EffectHelp(true, "Applies per tick.")
    case _ => EffectHelp(false, "Doesn't do anything.")
  }

  def itemEquipSkillOnlyHelp(context: EffectContext.Value) = context match {
    case Item => EffectHelp(true, "One-time effect of item use.")
    case Equipment => EffectHelp(true, "Occurs once per hit.")
    case Skill => EffectHelp(true, "One-time effect of skill use.")
    case _ => EffectHelp(false, "Doesn't do anything.")
  }

  def elementResistHelp(context: EffectContext.Value) = context match {
    case CharacterClass => EffectHelp(true, "Permanently has resistance.")
    case Equipment => EffectHelp(true, "Confers resistance on equipper.")
    case StatusEffect => EffectHelp(true, "Confers resistance while active.")
    case _ => EffectHelp(false, "Doesn't do anything.")
  }

  def onItemAndSkillUseOnlyHelp(context: EffectContext.Value) = context match {
    case Item => EffectHelp(true, "One-time effect of item use.")
    case Skill => EffectHelp(true, "One-time effect of skill use.")
    case _ => EffectHelp(false, "Doesn't do anything.")
  }

  val RecoverHpAdd = Val("Recover HP", help = recoveryHelp)
  val RecoverHpMul = Val("Recover percentage of HP", help = recoveryHelp)
  val RecoverMpAdd = Val("Recover MP", help = recoveryHelp)
  val RecoverMpMul = Val("Recover percentage of MP", help = recoveryHelp)


  val AddStatusEffect =
    Val("Add status effect", getEnumOfValue1(_.enums.statusEffects),
        help = itemEquipSkillOnlyHelp)

  val RemoveStatusEffect =
    Val("Remove status effect", getEnumOfValue1(_.enums.statusEffects),
        help = itemEquipSkillOnlyHelp)

  val MhpAdd = Val("Increase Max HP")
  val MmpAdd = Val("Increase Max MP")
  val AtkAdd = Val("Increase Attack")
  val SpdAdd = Val("Increase Speed")
  val MagAdd = Val("Increase Magic")
  val ArmAdd = Val("Increase Armor")
  val MreAdd = Val("Increase Magic Resist")

  val ElementResist = Val("Resist Element", getEnumOfValue2(_.enums.elements),
      help = elementResistHelp)

  val EscapeBattle = Val("Escape battle", help = onItemAndSkillUseOnlyHelp)
  val UseSkill = Val("Use skill", getEnumOfValue1(_.enums.skills),
      help = itemEquipSkillOnlyHelp)
  val LearnSkill = Val("Learn skill", getEnumOfValue1(_.enums.skills),
      help = onItemAndSkillUseOnlyHelp)

  def default = RecoverHpAdd

  def defaultEffect = Effect(RecoverHpAdd.id, 0, 0)
}