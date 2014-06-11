package rpgboss.model.battle

import rpgboss.model._

object BattleEntityType extends Enumeration {
  type BattleEntityType = Value
  val Party, Enemy = Value
}

/**
 * @param onAttackSkillIds      Usually there will be only one. However, it is 
 *                              an array to support dual-wielding.
 * @param row                   0 for front row. 1 for back row. Other values 
 *                              are undefined.
 */
class BattleStatus(
  pData: ProjectData,
  val entityType: BattleEntityType.Value,
  val id: Int,
  var hp: Int,
  var mp: Int,
  val baseStats: BaseStats,
  val equipment: Array[Int] = Array(),
  val onAttackSkillIds: Array[Int],
  private var tempStatusEffects: Array[Int],
  val row: Int) {
  
  def alive = hp > 0
  
  def update(deltaSeconds: Double, baseTurnTime: Double) = {
    if (!alive)
      readiness = 0
    
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
  def update(battle: Battle): Unit = {
    val alivePartyMembers = battle.partyStatus.filter(_.alive)
    // Do nothing and await Game Over if there are no alive party members.
    if (alivePartyMembers.isEmpty)
      return
    
    for (enemyStatus <- battle.readyEnemies) {
      // Randomly select a target among the alive party members.
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

case class BattleActionNotification(
  action: BattleAction, damages: Array[TakenDamage]) {
  override def toString(): String = {
    "BattleActionNotification(%s, %s)".format(action, damages.deep)
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
  
  private var _currentNotification: Option[BattleActionNotification] = None
  
  def getNotification = _currentNotification
  def dismissNotification() = 
    _currentNotification = None
  
  /**
   * BattleActions that have been queued up, but have not yet executed.
   * They live in a queue because we want actions to occur and be seen
   * by the player sequentially rather than all at once.
   */
  private val actionQueue = new collection.mutable.Queue[BattleAction]()
  
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
  def takeAction(action: BattleAction): Unit = {
    // Dequeue from readiness queue.
    val dequeued = readyQueue.dequeueFirst(_ == action.actor)
    
    // If actor is no longer in the ready queue, it is unable to act. It is 
    // probably dead.
    if (dequeued.isEmpty)
      return
    
    // Enqueue the actual action
    actionQueue.enqueue(action)
    
    // Remove readiness from actor
    action.actor.readiness = 0
  }
  
  val partyStatus: Array[BattleStatus] = {
    for (id <- partyIds) yield {
      val character = pData.enums.characters(id)
      val baseStats = character.baseStats(pData, characterLevels(id))
      
      val allItems = pData.enums.items
      val weaponSkills = 
        characterEquip(id)
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
        
      new BattleStatus(pData, BattleEntityType.Party, id,
                       initialCharacterHps(id), initialCharacterMps(id),
                       baseStats, characterEquip(id),
                       onAttackSkills, initialCharacterTempStatusEffects(id),
                       characterRows(id))
    }
  }
  val enemyStatus: Array[BattleStatus] = {
  
    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val enemy = pData.enums.enemies(unit.enemyIdx)
      val baseStats = enemy.baseStats
      val row = (i * 2) / encounter.units.length
      new BattleStatus(pData, BattleEntityType.Enemy, i, baseStats.mhp, 
                       baseStats.mmp, baseStats, 
                       equipment = Array(), 
                       onAttackSkillIds = Array(enemy.attackSkillId),
                       tempStatusEffects = Array(), 
                       row)
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
    advanceTime(0)
  }
  
  def advanceTime(deltaSeconds: Double) = {
    time += deltaSeconds
    
    allStatus.foreach(_.update(deltaSeconds, baseTurnTime))
    
    // Enqueue any newly ready entities.
    allStatus
      .filter(_.readiness >= 1.0)
      .sortBy(-_.readiness)
      .filter(!readyQueue.contains(_))
      .foreach(readyQueue.enqueue(_))
      
    aiOpt.map(_.update(this))
    
    // Only do an action if there's no outstanding notification.
    if (!actionQueue.isEmpty && _currentNotification.isEmpty) {
      val action = actionQueue.dequeue()
      val damages = action.process(this)
      _currentNotification = Some(
        BattleActionNotification(action, damages))
    }
    
    // Remove dead items from the ready queue.
    readyQueue.dequeueAll(!_.alive)
  }
    
}