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
  
  def update(deltaSeconds: Double, baseTurnTime: Double) = {
    val turnTime = baseTurnTime / (1.0 + stats.spd / 100.0)
    readiness += deltaSeconds / turnTime
  }
  
  var readiness: Double = 0
    
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
   * Simulation events that have been queued up, but have not yet taken place.
   * Ordering is by negative time, as we want events processed in time order.
   * There should not be any duplicate elements in this queue.
   */
  private val eventQueue = 
    new collection.mutable.PriorityQueue[TimestampedBattleEvent]()(
        Ordering.by(-_.time))
  
  /**
   * Battle entities, player characters and enemies, queued in order of 
   * readiness.
   */
  private val readyQueue = new collection.mutable.Queue[BattleStatus]
  
  /**
   * The first item in the ready queue that is ready to act.
   */
  def readyEntity = readyQueue.headOption
  
  /**
   * Enqueues up an action to be taken. Also removes the actor from the ready
   * queue.
   */
  def takeAction(action: BattleAction) = {
    // Dequeue from readiness queue.
    val dequeued = readyQueue.dequeueFirst(_ == action.actor)
    assert(dequeued.isDefined)
    
    // Enqueue the actual action
    eventQueue.enqueue(TimestampedBattleEvent(time, action))
    
    // Remove readiness from actor
    action.actor.readiness = 0
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
    val slowestToFastest = allStatus.sortBy(_.stats.spd)
    for ((status, i) <- slowestToFastest.zipWithIndex) {
      status.readiness = i.toDouble / (slowestToFastest.length - 1)
    }
    
    // Initialize ready queue
    update(0)
  }
  
  def update(deltaSeconds: Double) = {
    time += deltaSeconds
    
    allStatus.foreach(_.update(deltaSeconds, baseTurnTime))
    
    // Enqueue any newly ready entities.
    allStatus
      .filter(_.readiness >= 1.0)
      .sortBy(-_.readiness)
      .filter(!readyQueue.contains(_))
      .foreach(readyQueue.enqueue(_))
    
    while (!eventQueue.isEmpty && eventQueue.head.time <= time) {
      val event = eventQueue.dequeue().action
      event.process(this)
    }
  }
    
}