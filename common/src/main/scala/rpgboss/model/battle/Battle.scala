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
  val equipment: Array[Int] = Array(),
  private var tempStatusEffects: Array[Int],
  val row: Int) {
  
  def alive = hp > 0
  
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

trait BattleAI {
  def update(battle: Battle)
}

/**
 * This simple AI causes every ready enemy to randomly use a skill or an attack
 * on a random target.
 */
class RandomEnemyAI extends BattleAI {
  def update(battle: Battle) = {
    for (enemyStatus <- battle.readyEnemies) {
      // Randomly select a target among the alive party members.
      val alivePartyMembers = battle.partyStatus.filter(_.alive)
      assert(!alivePartyMembers.isEmpty)
      val target = 
        alivePartyMembers.apply(util.Random.nextInt(alivePartyMembers.length))
      
      val useSkill = util.Random.nextDouble() < 0.5
      if (useSkill) {
        assert(enemyStatus.id < battle.encounter.units.length)
        val encounterUnit = battle.encounter.units(enemyStatus.id)
        
        assert(encounterUnit.enemyIdx < battle.pData.enums.enemies.length)
        val enemy = battle.pData.enums.enemies(encounterUnit.enemyIdx)
        
        val skillIds = enemy.skills
        assert(enemy.skills.forall(i => i < battle.pData.enums.skills.length))
        
        // Select a skill at random that the unit has enough mana for. If there
        // are none, just attack.
        val canAffordSkills = skillIds.filter(skillId => {
          battle.pData.enums.skills(skillId).cost <= enemyStatus.mp
        })
          
        if (canAffordSkills.isEmpty) {
          battle.takeAction(AttackAction(enemyStatus, target))
        } else {
          val skillId = 
            canAffordSkills.apply(util.Random.nextInt(canAffordSkills.length))
          battle.takeAction(SkillAction(enemyStatus, target, skillId))
        }
      } else {
        battle.takeAction(AttackAction(enemyStatus, target))
      }
    }
  }
}

/**
 * @param   characterLevels         The levels of all the characters, not just 
 *                                  the ones within partyIds.
 * @param   initialCharacterHps     Array of HPs for all the characters.
 */
class Battle(
  val pData: ProjectData,
  val partyIds: Array[Int],
  characterLevels: Array[Int],
  initialCharacterHps: Array[Int],
  initialCharacterMps: Array[Int],
  characterEquip: Array[Array[Int]],
  initialCharacterTempStatusEffects: Array[Array[Int]],
  characterRows: Array[Int],
  val encounter: Encounter,
  aiOpt: Option[BattleAI]) {
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
   * The first item in the ready queue.
   */
  def readyEntity = readyQueue.headOption
  
  /**
   * All the ready enemies.
   */
  def readyEnemies = readyQueue.filter(_.entityType == BattleEntityType.Enemy)
  
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
  
  val partyStatus: Array[BattleStatus] = {
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
  val enemyStatus: Array[BattleStatus] = {
  
    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val baseStats = pData.enums.enemies(unit.enemyIdx).baseStats
      val row = (i * 2) / encounter.units.length
      new BattleStatus(pData, BattleEntityType.Enemy, i, baseStats.mhp, 
                       baseStats.mmp, baseStats, Array(), Array(), row)
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
      
    aiOpt.map(_.update(this))
    
    while (!eventQueue.isEmpty && eventQueue.head.time <= time) {
      val event = eventQueue.dequeue().action
      event.process(this)
    }
  }
    
}