package rpgboss.model

import rpgboss.lib._
import rpgboss.model.battle.BattleStatus
import rpgboss.model.battle.Hit

/**
 * Because effects have different meanings in different contexts, we provide
 * a way to get the validity and meaning of an effect in this context.
 */
object EffectContext extends Enumeration {
  val CharacterClass, Item, Equipment, Enemy, Skill, StatusEffect = Value
}

case class EffectUsability(valid: Boolean, helpMessage: String)

case class Effect(var keyId: Int, v1: Int = 0, v2: Int = 0) {
  def meta = Effect.getMeta(keyId)
  def applyToStats(stats: BattleStats) = meta.applyToStats(this, stats)
  def applyAsSkillOrItem(target: BattleStatus) =
    meta.applyAsSkillOrItem(this, target)

  // TODO: Remove this statement and remove var. This is to support legacy
  // mappings.
  keyId = meta.id
}

trait MetaEffect {
  def id: Int

  // TODO: Remove the getMeta clause. Only here to support legacy mappings.
  def matchesKey(keyId: Int): Boolean =
    keyId == id || Effect.getMeta(keyId).id == id
  def matches(effect: Effect) = matchesKey(effect.keyId)

  def name: String
  def usability = (context: EffectContext.Value) =>
    EffectUsability(false, "TODO: Implement a help message here.")
  def renderer = (pData: ProjectData, effect: Effect) => "TODO: No renderer"

  def applyToStats(effect: Effect, stats: BattleStats) = stats

  /**
   * Although this method looks similar to applyToStats, it's very different.
   * The method operates by mutating |target| directly without making a copy.
   * @return  The damage (or healing) performed. Used for display purposes.
   */
  def applyAsSkillOrItem(
      effect: Effect, target: BattleStatus): Seq[Damage] = Nil

  Effect.registerMetaEffect(id, this)
}

object Effect {
  import EffectContext._

  private var _metaEffects = collection.mutable.Map[Int, MetaEffect]()
  def registerMetaEffect(id: Int, metaEffect: MetaEffect) = {
    assert(!_metaEffects.contains(id))
    _metaEffects.put(id, metaEffect)
  }

  {
    // Serves at a static initializer to create all Effect classes and force
    // them to register their ids.
    // TODO: This seems hacky. There must be a better way...
    Array(
        RecoverHpAdd,
        RecoverHpMul,
        RecoverMpAdd,
        RecoverMpMul,

        AddStatusEffect,
        RemoveStatusEffect,
        RemoveAllStatusEffect,

        MhpAdd,
        MmpAdd,
        AtkAdd,
        SpdAdd,
        MagAdd,
        ArmAdd,
        MreAdd,

        ResistElement,

        EscapeBattle,

        LearnSkill,
        UseSkill
    )
  }

  /**
   * These are the old keyIds that we will be phasing out.
   * These were problematic because they were arbitrarily assigned, and have
   * no room between them to add new keys.
   */
  def getMeta(keyId: Int) = keyId match {
    case 0 => RecoverHpAdd
    case 1 => RecoverHpMul
    case 2 => RecoverMpAdd
    case 3 => RecoverMpMul

    case 4 => AddStatusEffect
    case 5 => RemoveStatusEffect

    case 6 => MhpAdd
    case 7 => MmpAdd
    case 8 => AtkAdd
    case 9 => SpdAdd
    case 10 => MagAdd
    case 11 => ArmAdd
    case 12 => MreAdd

    case 13 => ResistElement

    case 14 => EscapeBattle

    case 15 => LearnSkill
    case 16 => UseSkill

    case i => _metaEffects.getOrElse(i, InvalidEffect)
  }

  def pointRenderer(pData: ProjectData, effect: Effect) =
    "%dp".format(effect.v1)
  def percentRenderer(pData: ProjectData, effect: Effect) =
    "%d%%".format(effect.v1)

  /**
   * Renders the value of the enum index stored in v1.
   */
  def getEnumOfValue1[T <: HasName]
      (getChoices: ProjectData => Array[T])
      (pData: ProjectData, effect: Effect) = {
    val choices = getChoices(pData)
    val name =
      if (effect.v1 < choices.length)
        choices(effect.v1).name
      else
        "<Past end of array>"
    StringUtils.standardIdxFormat(effect.v1, name)
  }

  /**
   * Renders the value of the enum index stored in id, and then shows the number
   * stored in value.
   */
  def getEnumWithExtraValue[T <: HasName]
      (getChoices: ProjectData => Array[T])
      (pData: ProjectData, effect: Effect) = {
    val value1string = getEnumOfValue1(getChoices)(pData, effect)
    "%s. Value = %d ".format(value1string, effect.v2)
  }

  def recoveryHelp(context: EffectContext.Value) = context match {
    case Item => EffectUsability(true, "One-time effect of item use.")
    case Skill => EffectUsability(true, "One-time effect of skill use.")
    case StatusEffect => EffectUsability(true, "Applies per tick.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  def itemEquipSkillOnlyHelp(context: EffectContext.Value) = context match {
    case Item => EffectUsability(true, "One-time effect of item use.")
    case Equipment => EffectUsability(true, "Occurs once per hit.")
    case Skill => EffectUsability(true, "One-time effect of skill use.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  def classEquipOrStatus(context: EffectContext.Value) = context match {
    case CharacterClass => EffectUsability(true, "Permanently has resistance.")
    case Equipment => EffectUsability(true, "Confers resistance on equipper.")
    case StatusEffect => EffectUsability(true, "Confers resistance while active.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  def onItemAndSkillHelp(context: EffectContext.Value) = context match {
    case Item => EffectUsability(true, "One-time effect of item use.")
    case Skill => EffectUsability(true, "One-time effect of skill use.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  def onItemOnlyHelp(context: EffectContext.Value) = context match {
    case Item => EffectUsability(true, "One-time effect of item use.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  def onItemAndEquipHelp(context: EffectContext.Value) = context match {
    case Item => EffectUsability(true, "One-time effect of item use.")
    case Equipment => EffectUsability(true, "Occurs once per hit.")
    case _ => EffectUsability(false, "Doesn't do anything.")
  }

  /**
   * Recovery Utility functions
   */
  def recoverHp(target: BattleStatus, amount: Double): Seq[Damage] = {
    if (!target.alive)
      return Nil

    val amountInt = amount.round.toInt
    target.hp += amountInt
    List(Damage(DamageType.Magic, 0, -amountInt))
  }

  def recoverMp(target: BattleStatus, amount: Double) = {
    val amountInt = amount.round.toInt
    target.mp += amountInt
    List(Damage(DamageType.MPDamage, 0, -amountInt))
  }
}

object InvalidEffect extends MetaEffect {
  def id = -1
  def name = "Invalid Effect"
}

object RecoverHpAdd extends MetaEffect {
  def id = 100
  def name = "Recover HP"
  override def usability = Effect.recoveryHelp _
  override def renderer = Effect.pointRenderer _
  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) =
    Effect.recoverHp(target, effect.v1)
}

object RecoverHpMul extends MetaEffect {
  def id = 101
  def name = "Recover percentage of HP"
  override def renderer = Effect.percentRenderer _
  override def usability = Effect.recoveryHelp _
  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) =
    Effect.recoverHp(target, effect.v1 * 0.01 * target.stats.mhp)
}

object RecoverMpAdd extends MetaEffect {
  def id = 102
  def name = "Recover MP"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.recoveryHelp _
  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) =
    Effect.recoverMp(target, effect.v1)
}

object RecoverMpMul extends MetaEffect {
  def id = 103
  def name = "Recover percentage of MP"
  override def renderer = Effect.percentRenderer _
  override def usability = Effect.recoveryHelp _
  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) =
    Effect.recoverMp(target, effect.v1 * 0.01 * target.stats.mmp)
}

object AddStatusEffect extends MetaEffect {
  def id = 200
  def name = "Add status effect"
  override def renderer = Effect.getEnumWithExtraValue(_.enums.statusEffects) _
  override def usability = Effect.itemEquipSkillOnlyHelp _
  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) = {
    if (Utils.randomWithPercent(effect.v2)) {
      target.updateTempStatusEffectIds(target.tempStatusEffectIds :+ effect.v1)
      List(Damage(DamageType.AddStatusEffect, 0, effect.v1))
    } else {
      Nil
    }
  }
}

object RemoveStatusEffect extends MetaEffect {
  def id = 201
  def name = "Remove status effect"
  override def renderer = Effect.getEnumWithExtraValue(_.enums.statusEffects) _
  override def usability = Effect.itemEquipSkillOnlyHelp _

  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) = {
    if (Utils.randomWithPercent(effect.v2)) {
      target.updateTempStatusEffectIds(
          target.tempStatusEffectIds.filter(_ != effect.v1))
      List(Damage(DamageType.AddStatusEffect, 0, effect.v1))
    } else {
      Nil
    }
  }
}

object RemoveAllStatusEffect extends MetaEffect {
  def id = 202
  def name = "Remove all status effects"
  override def usability = Effect.itemEquipSkillOnlyHelp _

  override def applyAsSkillOrItem(effect: Effect, target: BattleStatus) = {
    val origEffects = target.tempStatusEffectIds
    if (!origEffects.isEmpty && Utils.randomWithPercent(effect.v2)) {
      target.updateTempStatusEffectIds(Array())
      origEffects.distinct.map(Damage(DamageType.AddStatusEffect, 0, _))
    } else {
      Nil
    }
  }
}

object MhpAdd extends MetaEffect {
  def id = 300
  def name = "Increase Max HP"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(mhp = stats.mhp + effect.v1)
}

object MmpAdd extends MetaEffect {
  def id = 301
  def name = "Increase Max MP"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(mmp = stats.mmp + effect.v1)
}

object AtkAdd extends MetaEffect {
  def id = 302
  def name = "Increase ATK"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(atk = stats.atk + effect.v1)
}

object SpdAdd extends MetaEffect {
  def id = 303
  def name = "Increase SPD"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(spd = stats.spd + effect.v1)
}

object MagAdd extends MetaEffect {
  def id = 304
  def name = "Increase MAG"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(mag = stats.mag + effect.v1)
}

object ArmAdd extends MetaEffect {
  def id = 305
  def name = "Increase ARM"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(arm = stats.arm + effect.v1)
}

object MreAdd extends MetaEffect {
  def id = 306
  def name = "Increase MRE"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) =
    stats.copy(mre = stats.mre + effect.v1)
}

object ResistElement extends MetaEffect {
  def id = 400
  def name = "Resist Element"
  override def renderer = Effect.getEnumWithExtraValue(_.enums.statusEffects) _
  override def usability = Effect.classEquipOrStatus _
  override def applyToStats(effect: Effect, stats: BattleStats) = {
    val newResists = stats.elementResists.updated(
      effect.v1, stats.elementResists(effect.v1) + effect.v2)
    stats.copy(elementResists = newResists)
  }
}

object EscapeBattle extends MetaEffect {
  def id = 500
  def name = "Escape Battle"
  override def renderer = Effect.pointRenderer _
  override def usability = Effect.onItemOnlyHelp _
}

object LearnSkill extends MetaEffect {
  def id = 600
  def name = "Learn Skill"
  override def renderer = Effect.getEnumOfValue1(_.enums.skills) _
  override def usability = Effect.onItemOnlyHelp _
}

object UseSkill extends MetaEffect {
  def id = 601
  def name = "Use Skill"
  override def renderer = Effect.getEnumOfValue1(_.enums.skills) _
  override def usability = Effect.onItemAndEquipHelp _
}