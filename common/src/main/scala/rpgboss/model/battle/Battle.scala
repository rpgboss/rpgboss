package rpgboss.model.battle

import rpgboss.model._

object BattleEntityType extends Enumeration {
  type BattleEntityType = Value
  val Party, Enemy = Value
}

/**
 * @param row     0 for front row. 1 for back row. Other values are undefined.
 */
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
    
  private var _stats = 
    BattleStats(pData, baseStats, equipment, tempStatusEffects)
    
  def stats = _stats
  
  override def toString = "BattleStatus(%s, %d)".format(entityType, id)
}

/**
 * @param   characterLevels         The levels of all the characters, not just 
 *                                  the ones within partyIds.
 * @param   initialCharacterHps     Array of HPs for all the characters.
 */
class Battle(
  val pData: ProjectData,
  val partyIds: Seq[Int],
  characterLevels: Seq[Int],
  initialCharacterHps: Seq[Int],
  initialCharacterMps: Seq[Int],
  characterEquip: Seq[Seq[Int]],
  initialCharacterTempStatusEffects: Seq[Seq[Int]],
  characterRows: Seq[Int],
  val encounter: Encounter) {
  require(partyIds.forall(i => i >= 0 && i < pData.enums.characters.length))
  require(encounter.units.forall(
    unit => unit.enemyIdx >= 0 && unit.enemyIdx < pData.enums.enemies.length))
  
  private var time = 0.0
  
  /**
   * How many seconds it takes an actor with 0 speed to get a new turn.
   */
  val baseTurnTime = 4.0
  
  /**
   * Time separation between initial ready times
   */
  val readySeparation = baseTurnTime * 0.3
  
  /**
   * Simulation events that have been queued up, but have not yet taken place.
   * Ordering is by negative time, as we want events processed in time order.
   * There should not be any duplicate elements in this queue.
   */
  private val eventQueue = 
    new collection.mutable.PriorityQueue[TimestampedBattleEvent]()(
        Ordering.by(-_.time))
  
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
   * Created here to allow for access to private readyQueue variable.
   */
  def newReadyEvent(entity: BattleStatus) = new BattleEvent {
    def process(battle: Battle) = {
      readyQueue.enqueue(entity)
    }
  }
  
  /**
   * Enqueues up an action to be taken. Also removes the actor from the ready
   * queue.
   */
  def takeAction(action: BattleAction) = {
    // Remove the action taker from list of ready actors
    val dequeued = readyQueue.dequeueFirst(_ == action.actor)
    assert(dequeued.isDefined)
    
    // Enqueue the actual action
    eventQueue.enqueue(TimestampedBattleEvent(time, action))
    
    // Enqueue next ready time
    val turnTime = baseTurnTime / (1.0 + action.actor.stats.spd / 100.0)
    eventQueue.enqueue(
        TimestampedBattleEvent(time + turnTime, newReadyEvent(action.actor)))
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
    val fastestToSlowest = allStatus.sortBy(-_.stats.spd)
    
    for ((status, i) <- fastestToSlowest.zipWithIndex) {
      eventQueue.enqueue(TimestampedBattleEvent(
          time + i * baseTurnTime * 0.3, newReadyEvent(status)))
    }
    
    // Initialize ready queue
    update(0)
  }
  
  def update(deltaSeconds: Double) = {
    time += deltaSeconds
    
    while (!eventQueue.isEmpty && eventQueue.head.time <= time) {
      val event = eventQueue.dequeue().action
      event.process(this)
    }
  }
    
}