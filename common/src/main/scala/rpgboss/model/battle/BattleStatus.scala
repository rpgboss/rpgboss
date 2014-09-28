package rpgboss.model.battle

import rpgboss.model.BattleStats
import rpgboss.model.BaseStats
import rpgboss.model.ProjectData
import rpgboss.lib.Utils

object BattleEntityType extends Enumeration {
  type BattleEntityType = Value
  val Party, Enemy = Value
}

/**
 * This class is used within Battle to keep track of the state of entities
 * within battle. It may be also used outside of battle when applying item
 * effects from the menu.
 *
 * @param index                 Used to keep track of which index in the list of
 *                              BattleStatus's for a given Battle.
 * @param entityIndex           Is the character index or enemy index within
 *                              the project's list of characters/enemies.
 * @param onAttackSkillIds      Usually there will be only one. However, it is
 *                              an array to support dual-wielding.
 * @param row                   0 for front row. 1 for back row. Other values
 *                              are undefined.
 */
class BattleStatus(
  val index: Int,
  pData: ProjectData,
  val entityType: BattleEntityType.Value,
  val entityId: Int,
  var hp: Int,
  var mp: Int,
  val baseStats: BaseStats,
  val equipment: Array[Int] = Array(),
  val onAttackSkillIds: Array[Int],
  val knownSkillIds: Array[Int],
  initialTempStatusEffects: Array[Int],
  val row: Int) {

  def alive = hp > 0

  private var _tempStatusEffects = initialTempStatusEffects

  def calculateStats() =
    BattleStats(pData, baseStats, equipment, _tempStatusEffects)

  private var _stats = calculateStats()

  def updateTempStatusEffects(newTempStatusEffects: Array[Int]) = {
    _tempStatusEffects = newTempStatusEffects;
    _stats = calculateStats()
  }

  def clampVitals() = {
    hp = Utils.clamped(hp, 0, stats.mhp)
    mp = Utils.clamped(mp, 0, stats.mmp)
  }

  def update(pendingAction: Boolean, deltaSeconds: Double,
             baseTurnTime: Double) = {
    if (!alive) {
      readiness = 0
    } else if (!pendingAction) {
      val turnTime = baseTurnTime / (1.0 + stats.spd / 100.0)
      readiness += deltaSeconds / turnTime
    }
  }

  var readiness: Double = 0

  def stats = _stats
  def tempStatusEffects = _tempStatusEffects

  override def toString = "BattleStatus(%s, %d)".format(entityType, index)
}

object BattleStatus {
  def fromCharacter(
    pData: ProjectData,
    partyParams: PartyParameters,
    characterId: Int,
    index: Int) = {
    val character = pData.enums.characters(characterId)
    val level = partyParams.characterLevels(characterId)
    val baseStats = character.baseStats(pData, level)

    val allItems = pData.enums.items
    val weaponSkills =
      partyParams
        .characterEquip(characterId)
        .filter(_ < allItems.length)
        .map(id => allItems(id).onUseSkillId)
    val onAttackSkills =
      if (weaponSkills.size == 0) {
        assume(character.charClass < pData.enums.classes.size)
        val charClass = pData.enums.classes(character.charClass)
        Array(charClass.unarmedAttackSkillId)
      } else {
        weaponSkills
      }

    assert(character.charClass < pData.enums.classes.length)
    val knownSkillIds =
      pData.enums.classes(character.charClass).knownSkillIds(level)

    new BattleStatus(
      index, pData, BattleEntityType.Party, characterId,
      partyParams.initialCharacterHps(characterId),
      partyParams.initialCharacterMps(characterId),
      baseStats,
      partyParams.characterEquip(characterId),
      onAttackSkills,
      knownSkillIds,
      partyParams.initialCharacterTempStatusEffects(characterId),
      partyParams.characterRows(characterId))
  }
}