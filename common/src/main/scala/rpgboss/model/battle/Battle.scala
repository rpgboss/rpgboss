package rpgboss.model.battle

import rpgboss.model._

object BattleEntityType extends Enumeration {
  type BattleEntityType = Value
  val Party, Enemy = Value
}

class BattleStatus(
  pData: ProjectData,
  val entityType: BattleEntityType.Value,
  val id: Int,
  var hp: Int,
  var mp: Int,
  val baseStats: BaseStats,
  val equipment: Seq[Int] = Seq(),
  private var tempStatusEffects: Seq[Int],
  val row: Int) {
  
  /**
   * Varies between 0.0 and +inf. At 1.0 and greater, entity is ready to act.
   */
  var turnsSinceLastAction = 0.0
  
  /**
   * True if the Battle has already put this entity into the ready queue.
   */
  var putInReadyQueue = false
  
  private var _stats = 
    BattleStats(pData, baseStats, equipment, tempStatusEffects)
    
  def stats = _stats
}

case class BattleAction(source: BattleStatus)

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
  
  /**
   * How many seconds it takes an actor with 0 speed to get a new turn.
   */
  val baseTurnTime = 4.0
  
  /**
   * Actions that have been queued up by the player or AI, but have not yet
   * taken place.
   */
  private val actionQueue = new collection.mutable.Queue[BattleAction]
  
  /**
   * Battle entities, player characters and enemies, that are ready to act but
   * have not yet acted.
   */
  private val readyQueue = new collection.mutable.Queue[BattleStatus]
  
  /**
   * The first item in the ready queue.
   */
  def readyEntity = readyQueue.headOption
  
  /**
   * Enqueues up an action to be taken. Also removes the actor from the ready
   * queue.
   */
  def takeAction(action: BattleAction) = {
    val dequeued = readyQueue.dequeueFirst(_ == action.source)
    assert(dequeued.isDefined)
    action.source.turnsSinceLastAction = 0
    action.source.putInReadyQueue = false
    
    actionQueue.enqueue(action)
  }
  
  val partyStatus: Seq[BattleStatus] = {
    for (id <- partyIds) yield {
      val baseStats = 
        pData.enums.characters(id).baseStats(pData, characterLevels(id))
      new BattleStatus(pData, BattleEntityType.Party, id, 
                       initialCharacterHps(id), initialCharacterMps(id),
                       baseStats, characterEquip(id),
                       initialCharacterTempStatusEffects(id), 
                       characterRows(id))
    }
  }
  val enemyStatus: Seq[BattleStatus] = {
  
    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val baseStats = pData.enums.enemies(unit.enemyIdx).baseStats
      val row = (i * 2) / encounter.units.length
      new BattleStatus(pData, BattleEntityType.Enemy, i, baseStats.mhp, 
                       baseStats.mmp, baseStats, Seq(), Seq(), row)
    }
  }
  val allStatus = partyStatus ++ enemyStatus
  
  // Set the readiness level of all the participants. Simple linear algorithm.
  { 
    val sortedBySpeed = allStatus.sortBy(_.stats.spd)
    
    if (allStatus.length == 1) {
      allStatus.head.turnsSinceLastAction = 1.0
    } else {
      // Set the readiness level from 0.0 to 1.0 based on speed.
      for ((status, i) <- sortedBySpeed.zipWithIndex) {
        status.turnsSinceLastAction = i / (allStatus.length - 1)
      }
    }
    
    // Initialize ready queue
    update(0)
  }
  
  def update(deltaSeconds: Float) = {
    time += deltaSeconds
    val newlyReady = new collection.mutable.ArrayBuffer[BattleStatus]
    
    for (status <- allStatus) {
      val effectiveTurnTime = 
        1.0 / (1.0 + status.stats.spd.toDouble / 100.0) * baseTurnTime
      status.turnsSinceLastAction += deltaSeconds / effectiveTurnTime
      
      if (status.turnsSinceLastAction >= 1.0 && !status.putInReadyQueue) {
        newlyReady.append(status)
        status.putInReadyQueue = true
      }
    }
    
    // Add newly ready items in order
    newlyReady
      .sortBy(_.turnsSinceLastAction)
      .foreach(x => readyQueue.enqueue(x))
  }
    
}