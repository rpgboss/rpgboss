package rpgboss.model.battle

import rpgboss.model._
import rpgboss.player.PersistentState

/**
 * @param   animationId     Negative means no animation.
 */
case class Hit(hitActor: BattleStatus, damage: Damage, animationId: Int)

/**
 * A BattleAction is an action taken by an entity in the battle. It consumes
 * the initiative a.k.a. readiness of that entity and moves it to the back
 * of the ready queue.
 */
trait BattleAction {
  def actor: BattleStatus
  def targets: Array[BattleStatus]

  /**
   * Returns the hits, as well as the success of the action.
   */
  def process(battle: Battle): (Array[Hit], Boolean)
}

case class NullAction(actor: BattleStatus) extends BattleAction {
  override def targets = Array()
  def process(battle: Battle) = {
    (Array(), true)
  }
}

object StatusEffectAction extends BattleAction {
  override def actor = null
  override def targets = Array()
  def process(battle: Battle) = {
    (Array(), true)
  }
}

case class AttackAction(actor: BattleStatus, targets: Array[BattleStatus])
  extends BattleAction {
  def process(battle: Battle) = {
    assert(targets.length == 1)
    val desiredTarget = targets.head

    val actualTargets =
      if (desiredTarget.alive)
        Some(desiredTarget)
      else
        battle.randomAliveOf(desiredTarget.entityType)

    val hits = new collection.mutable.ArrayBuffer[Hit]
    for (target <- actualTargets; skillId <- actor.onAttackSkillIds) {
      assume(skillId < battle.pData.enums.skills.length)
      val skill = battle.pData.enums.skills(skillId)
      hits ++= skill.applySkill(actor, target)
    }

    (hits.toArray, true)
  }
}

case class SkillAction(actor: BattleStatus, targets: Array[BattleStatus],
                       skillId: Int)
  extends BattleAction {
  def process(battle: Battle): (Array[Hit], Boolean) = {
    assume(skillId < battle.pData.enums.skills.length)
    val skill = battle.pData.enums.skills(skillId)
    if (actor.mp < skill.cost)
      return (Array(), false)

    actor.mp -= skill.cost

    assert(targets.length >= 1)

    val hits = new collection.mutable.ArrayBuffer[Hit]
    for (target <- targets) {
      hits ++= skill.applySkill(actor, target)
    }

    (hits.toArray, true)
  }
}

case class ItemAction(actor: BattleStatus, targets: Array[BattleStatus],
                      itemId: Int, persistentState: PersistentState)
  extends BattleAction {
  def process(battle: Battle) = {
    assume(itemId < battle.pData.enums.items.length)
    val item = battle.pData.enums.items(itemId)

    val removed = persistentState.addRemoveItem(itemId, -1)

    if (removed) {
      assert(item.usableInBattle)
      assert(targets.length >= 1)

      val hits = new collection.mutable.ArrayBuffer[Hit]
      for (target <- targets) {
        val damages = item.effects.flatMap(_.applyAsSkillOrItem(target))

        // TODO: Add animations to items
        hits ++= damages.map(damage => Hit(target, damage, -1))
      }

      (hits.toArray, true)
    } else {
      (Array.empty, false)
    }
  }
}

case class EscapeAction(actor: BattleStatus) extends BattleAction {
  override def targets = Array()
  def process(battle: Battle) = {
    val success = battle.attemptEscape()
    (Array.empty, success)
  }
}