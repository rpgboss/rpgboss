package rpgboss.model.battle

import rpgboss.model._

case class BattleStatus(
  var hp: Int,
  var mp: Int,
  val baseStats: BaseStats,
  val equipment: Seq[Int] = Seq(),
  var tempStatusEffects: Seq[Int],
  val row: Int)

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
      BattleStatus(initialCharacterHps(id), initialCharacterMps(id),
                   baseStats, characterEquip(id),
                   initialCharacterTempStatusEffects(id), 
                   characterRows(id))
    }
  }
  
  val enemyStatus = {
    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val baseStats = pData.enums.enemies(unit.enemyIdx).baseStats
      val row = (i * 2) / encounter.units.length
      BattleStatus(baseStats.mhp, baseStats.mmp, baseStats, Seq(), Seq(), row)
    }
  }
  
}