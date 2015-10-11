package rpgboss.model.battle

import rpgboss.lib.Utils
import rpgboss.model.Encounter
import rpgboss.model.ProjectData
import scala.collection.mutable.ArrayBuffer
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BattleAI {
  def update(battle: Battle)
}

/**
 * This simple AI causes every ready enemy to randomly use a skill or an attack
 * on a random target.
 */
class RandomEnemyAI extends BattleAI {
  def update(battle: Battle): Unit = {
    for (enemyStatus <- battle.readyEnemies) {
      // Randomly select a target among the alive party members.
      val target = battle.randomAliveOf(BattleEntityType.Party)

      // Do nothing and await Game Over if there are no alive party members.
      if (target.isEmpty)
        return

      val useSkill = util.Random.nextDouble() < 0.5
      if (useSkill) {
        assert(enemyStatus.entityId < battle.pData.enums.enemies.length)
        val enemy = battle.pData.enums.enemies(enemyStatus.entityId)

        val skillIds = enemy.skillIds
        assert(enemy.skillIds.forall(i => i < battle.pData.enums.skills.length))

        // Select a skill at random that the unit has enough mana for. If there
        // are none, just attack.
        val canAffordSkills = skillIds.filter(skillId => {
          battle.pData.enums.skills(skillId).cost <= enemyStatus.mp
        })

        if (canAffordSkills.isEmpty) {
          battle.takeAction(AttackAction(enemyStatus, Array(target.get)))
        } else {
          val skillId =
            canAffordSkills.apply(util.Random.nextInt(canAffordSkills.length))
          val skill = battle.pData.enums.skills(skillId)

          battle.takeAction(SkillAction(enemyStatus, Array(target.get), skillId))
        }
      } else {
        battle.takeAction(AttackAction(enemyStatus, Array(target.get)))
      }
    }
  }
}

case class BattleActionNotification(
  action: BattleAction, hits: Array[Hit], success: Boolean) {
  override def toString(): String = {
    "BattleActionNotification(%s, %s)".format(action, hits.deep)
  }
}

case class PartyParameters(
  characterLevels: Array[Int],
  initialCharacterHps: Array[Int],
  initialCharacterMps: Array[Int],
  characterEquip: Array[Array[Int]],
  initialCharacterTempStatusEffectIds: Array[Array[Int]],
  learnedSkills: Array[Array[Int]],
  characterRows: Array[Int])

object Battle {
  val ACTIVE = 0
  val VICTORY = 1
  val DEFEAT = 2
  val ESCAPED = 3
}

/**
 * @param   characterLevels         The levels of all the characters, not just
 *                                  the ones within partyIds.
 * @param   initialCharacterHps     Array of HPs for all the characters.
 */
class Battle(
  val pData: ProjectData,
  val partyIds: Array[Int],
  partyParams: PartyParameters,
  val encounter: Encounter,
  aiOpt: Option[BattleAI]) extends LazyLogging {
  require(partyIds.forall(i => i >= 0 && i < pData.enums.characters.length))
  require(encounter.units.forall(
    unit => unit.enemyIdx >= 0 && unit.enemyIdx < pData.enums.enemies.length))

  private var _time = 0.0f
  def time = _time

  private var _state = Battle.ACTIVE
  def state = _state

  var scriptLastExecuted = Array.fill(encounter.events.length)(Float.MinValue)

  private def _enemyDatas =
    enemyStatus.map(status => pData.enums.enemies.apply(status.entityId))

  def victoryExperience = _enemyDatas.map(_.expValue).sum
  def goldDrops = _enemyDatas.map(_.droppedGold).sum

  def generateItemDrops() = {
    val droppedItemIds = new ArrayBuffer[Int]()
    for (enemy <- _enemyDatas) {
      val totalChanceDrop = enemy.droppedItems.map(_.chance).sum
      if (totalChanceDrop > 1.0f) {
        logger.warn("Enemy \"%s\" has a %f%% chance of dropping items.".format(
            enemy.name, totalChanceDrop * 100))
      }

      if (math.random < totalChanceDrop) {
        droppedItemIds.append(
            Utils.randomChoose(enemy.droppedItems.map(_.itemId),
                enemy.droppedItems.map(_.chance)))
      }
    }
    droppedItemIds
  }

  /**
   * How many seconds it takes an actor with 0 speed to get a new turn.
   */
  val baseTurnTime = 4.0f

  private var _notifications =
    collection.mutable.Queue[BattleActionNotification]()

  def getNotification = _notifications.headOption
  def dismissNotification() = _notifications.dequeue()

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
    for ((characterId, index) <- partyIds.zipWithIndex) yield {
      BattleStatus.fromCharacter(pData, partyParams, characterId, index)
    }
  }
  val enemyStatus: Array[BattleStatus] = {

    for ((unit, i) <- encounter.units.zipWithIndex) yield {
      val enemy = pData.enums.enemies(unit.enemyIdx)
      val baseStats = enemy.baseStats
      val row = (i * 2) / encounter.units.length
      new BattleStatus(i, pData, BattleEntityType.Enemy, unit.enemyIdx,
        baseStats.mhp, baseStats.mmp, baseStats,
        equipment = Array(),
        onAttackSkillIds = Array(enemy.attackSkillId),
        knownSkillIds = enemy.skillIds,
        initialTempStatusEffectIds = Array(),
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

  /**
   * Returns true if party was able to escape.
   */
  def attemptEscape(): Boolean = {
    if (encounter.canEscape && math.random < encounter.escapeChance) {
      _state = Battle.ESCAPED
      return true
    }
    return false
  }

  def randomAliveOf(entityType: BattleEntityType.Value) = {
    val aliveList = entityType match {
      case BattleEntityType.Enemy => enemyStatus.filter(_.alive)
      case BattleEntityType.Party => partyStatus.filter(_.alive)
      case _ => Array()
    }

    if (aliveList.isEmpty)
      None
    else
      Some(aliveList.apply(util.Random.nextInt(aliveList.length)))
  }

  def advanceTime(deltaSeconds: Float): Unit = {
    if (_state != Battle.ACTIVE)
      return

    _time += deltaSeconds
    // TODO: This method is wrong, but right enough most of the time.
    val ticked = (_time % baseTurnTime) <= deltaSeconds

    val statusEffectHits = for (status <- allStatus) yield {
      val pendingAction = actionQueue.exists(_.actor == status)

      status.update(pendingAction, deltaSeconds, baseTurnTime, ticked)
    }
    val flattenedStatusEffectHits = statusEffectHits.flatten
    if (!flattenedStatusEffectHits.isEmpty) {
      _notifications.enqueue(BattleActionNotification(
          StatusEffectAction, flattenedStatusEffectHits, true))
    }

    // Enqueue any newly ready entities.
    allStatus
      .filter(_.readiness >= 1.0)
      .sortBy(-_.readiness)
      .filter(!readyQueue.contains(_))
      .foreach(readyQueue.enqueue(_))

    aiOpt.map(_.update(this))

    // Only do an action if there's no outstanding notification.
    if (!actionQueue.isEmpty) {
      val action = actionQueue.dequeue()
      val (hits, success) = action.process(this)
      _notifications.enqueue(BattleActionNotification(action, hits, success))
    }

    // Remove dead items from the ready queue.
    readyQueue.dequeueAll(!_.alive)

    if (partyStatus.forall(!_.alive)) {
      _state = Battle.DEFEAT
      readyQueue.clear()
    } else if (enemyStatus.forall(!_.alive)) {
      _state = Battle.VICTORY
      readyQueue.clear()
    }
  }

}