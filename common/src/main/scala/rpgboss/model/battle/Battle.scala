package rpgboss.model.battle

import rpgboss.model._

object BattleEntityType extends Enumeration {
  type BattleEntityType = Value
  val Party, Enemy = Value
}

case class BattleStatus(
  pData: ProjectData,
  entityType: BattleEntityType.Value,
  id: Int,
  var hp: Int,
  var mp: Int,
  val baseStats: BaseStats,
  val equipment: Seq[Int] = Seq(),
  private var tempStatusEffects: Seq[Int],
  val row: Int) {
  
  /**
   * Varies between 0.0 and 1.0. At 1.0 (or greater), an entity can take a turn.
   */
  var readiness = 0.0
  
  private var _stats = 
    BattleStats(pData, baseStats, equipment, tempStatusEffects)
    
  def stats = _stats
}

class Battle(
  pData: ProjectData,
  partyIds: Seq[Int],
  characterLevels: Seq[Int],
  initialCharacterHps: Seq[Int],
  initialCharacterMps: Seq[Int],
  characterEquip: Seq[Seq[Int]],
  initialCharacterTempStatusEffects: Seq[Seq[Int]],
  characterRows: Seq[Int],
  encounter: Encounter) {
  require(partyIds.forall(i => i >= 0 && i < pData.enums.characters.length))
  require(encounter.units.forall(
    unit => unit.enemyIdx >= 0 && unit.enemyIdx < pData.enums.enemies.length))
  
  private var time = 0.0f
  
  val partyStatus: Seq[BattleStatus] = {
    for (id <- partyIds) yield {
      val baseStats = 
        pData.enums.characters(id).baseStats(pData, characterLevels(id))
      BattleStatus(pData, BattleEntityType.Party, id, initialCharacterHps(id), 
                   initialCharacterMps(id),
                   baseStats, characterEquip(id),
                   initialCharacterTempStatusEffects(id), 
                   characterRows(id))
    }
  }
  val enemyStatus: Seq[BattleStatus] = {
  
    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val baseStats = pData.enums.enemies(unit.enemyIdx).baseStats
      val row = (i * 2) / encounter.units.length
      BattleStatus(pData, BattleEntityType.Enemy, i, baseStats.mhp, 
                   baseStats.mmp, baseStats, Seq(), Seq(), row)
    }
  }
  
  // Set the readiness level of all the participants. Simple linear algorithm.
  { 
    val allParticipants = partyStatus ++ enemyStatus
    val sortedBySpeed = allParticipants.sortBy(_.stats.spd)
    
    if (allParticipants.length == 1) {
      allParticipants.head.readiness = 1.0
    } else {
      // Set the readiness level from 0.0 to 1.0 based on speed.
      for ((status, i) <- sortedBySpeed.zipWithIndex) {
        status.readiness = i / (allParticipants.length - 1)
      }
    }
  }
  
  
}