package rpgboss.model

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

case class StatusEffect(
  var name: String = "",
  var effects: Array[Effect] = Array(),
  var releaseOnBattleEnd: Boolean = false,
  var releaseTime: Int = 0,
  var releaseChance: Int = 0,
  var releaseDmgChance: Int = 0,
  var maxStacks: Int = 1) extends HasName

case class BattleStats(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  arm: Int,
  mre: Int,
  elementResists: Array[Int],
  statusEffects: Array[StatusEffect])

object BattleStats {
  def apply(pData: ProjectData, baseStats: BaseStats,
            equippedIds: Array[Int] = Array(),
            tempStatusEffectIds: Array[Int] = Array()): BattleStats = {
    require(equippedIds.forall(i => i >= 0 && i < pData.enums.items.length))
    require(tempStatusEffectIds.forall(
      i => i >= 0 && i < pData.enums.statusEffects.length))

    val equipment = equippedIds.map(pData.enums.items)
    val equipmentEffects = equipment.flatMap(_.effects)

    // Make sure we don't apply more than max-stacks of each status effect
    val stackedStatusEffects: Array[StatusEffect] = {
      val statusEffectStackMap = collection.mutable.Map[Int, Int]()
      val statusEffectBuffer = collection.mutable.ArrayBuffer[StatusEffect]()

      def isValidStatusId(id: Int) =
        id >= 0 && id < pData.enums.statusEffects.length
      val equipmentStatusEffectIds =
        equipmentEffects.filter(AddStatusEffect.matches).map(_.v1)

      for (statusEffectId <- equipmentStatusEffectIds ++ tempStatusEffectIds) {
        val statusEffect = pData.enums.statusEffects(statusEffectId)
        val currentCount =
          statusEffectStackMap.getOrElseUpdate(statusEffectId, 0)

        if (currentCount < statusEffect.maxStacks) {
          statusEffectStackMap.update(statusEffectId, currentCount + 1)
          statusEffectBuffer.append(statusEffect)
        }
      }

      statusEffectBuffer.toArray
    }

    val allEffects = baseStats.effects ++ equipmentEffects ++
                     stackedStatusEffects.flatMap(_.effects)

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

