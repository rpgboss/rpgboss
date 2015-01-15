package rpgboss.model

import rpgboss.model.battle.BattleStatus
import rpgboss.lib.Utils
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.collection.mutable.ArrayBuffer
import rpgboss.model.battle.Hit

case class Curve(var base: Float, var perLevel: Float) {
  def apply(x: Int): Int = {
    (perLevel * (x - 1) + base).round.toInt
  }
}

case class StatProgressions(
  var exp: Curve = Curve(300, 100), // Exp required to level up. Not cumulative.
  var mhp: Curve = Curve(50, 10),
  var mmp: Curve = Curve(20, 4),
  var atk: Curve = Curve(10, 2),
  var spd: Curve = Curve(10, 2),
  var mag: Curve = Curve(10, 2),
  var arm: Curve = Curve(10, 1),
  var mre: Curve = Curve(10, 1))

/** Holds the intrinsic stats of the entity without any equipment or temporary
 *  status effects.
 */
case class BaseStats(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  arm: Int,
  mre: Int,
  effects: Array[Effect])

/**
 * @param   releaseChancePerTick    Chance of wearing off per tick.
 */
case class StatusEffect(
  var name: String = "",
  var effects: Array[Effect] = Array(),
  var releaseChancePerTick: Int = 20,
  var maxStacks: Int = 1) extends HasName

case class StatusEffectSpec(
    id: Int, temporary: Boolean, statusEffect: StatusEffect)
    extends LazyLogging {
  /**
   * @param   id          The id of this status effect. Used for releasing.
   * @param   temporary   Used for random releasing.
   */
  def applyTick(target: BattleStatus): Array[Hit] = {
    logger.debug("Applying %s effect to %s".format(statusEffect.name, target))
    val tickHits =
      statusEffect.effects
        .flatMap(_.applyAsSkillOrItem(target).map(Hit(target, _, -1)))

    // Release a stack
    if (temporary && statusEffect.releaseChancePerTick > 0 &&
        Utils.randomWithPercent(statusEffect.releaseChancePerTick)) {
      logger.debug("  Releasing status effect.")
      val newTempStatusEffects = ArrayBuffer(target.tempStatusEffectIds: _*)
      newTempStatusEffects -= id
      target.updateTempStatusEffectIds(newTempStatusEffects.toArray)

      tickHits :+ Hit(target, Damage(DamageType.StatusEffect, 0, id), -1)
    } else {
      tickHits
    }
  }
}

case class BattleStats(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  arm: Int,
  mre: Int,
  elementResists: Array[Int],
  statusEffects: Array[StatusEffectSpec])

object BattleStats {
  def apply(pData: ProjectData, baseStats: BaseStats,
            equippedIds: Array[Int] = Array(),
            tempStatusEffectIds: Array[Int] = Array()): BattleStats = {
    // -1 is okay because that signifies no item equipped.
    require(equippedIds.forall(i => i >= -1 && i < pData.enums.items.length))
    require(tempStatusEffectIds.forall(
      i => i >= 0 && i < pData.enums.statusEffects.length))

    val equipment = equippedIds.filterNot(_ == -1).map(pData.enums.items)
    val equipmentEffects = equipment.flatMap(_.effects)

    // Make sure we don't apply more than max-stacks of each status effect
    val stackedStatusEffects: Array[StatusEffectSpec] = {
      val statusEffectStackMap = collection.mutable.Map[Int, Int]()
      val statusEffectBuffer =
        collection.mutable.ArrayBuffer[StatusEffectSpec]()

      val equipmentStatusEffectIds =
        equipmentEffects.filter(AddStatusEffect.matches).map(_.v1)

      def addIfValid(id: Int, temporary: Boolean) = {
        assert(id >= 0 && id < pData.enums.statusEffects.length)

        val statusEffect = pData.enums.statusEffects(id)
        val currentCount = statusEffectStackMap.getOrElseUpdate(id, 0)

        if (currentCount < statusEffect.maxStacks) {
          statusEffectStackMap.update(id, currentCount + 1)
          statusEffectBuffer.append(
              StatusEffectSpec(id, temporary, statusEffect))
        }
      }

      equipmentStatusEffectIds.foreach(addIfValid(_, false))
      tempStatusEffectIds.foreach(addIfValid(_, true))
      statusEffectBuffer.toArray
    }

    val allEffects = baseStats.effects ++ equipmentEffects ++
                     stackedStatusEffects.flatMap(_.statusEffect.effects)

    val statsWithoutEffects = apply(
      mhp = baseStats.mhp,
      mmp = baseStats.mmp,
      atk = baseStats.atk,
      spd = baseStats.spd,
      mag = baseStats.mag,
      arm = baseStats.arm,
      mre = baseStats.mre,
      elementResists = Array.fill(pData.enums.elements.size)(0),
      statusEffects = stackedStatusEffects
    )

    // Apply list of effects to stats repeatedly without notion of order.
    allEffects.foldLeft(statsWithoutEffects)(
      (stats, effect) => effect.applyToStats(stats))
  }
}

