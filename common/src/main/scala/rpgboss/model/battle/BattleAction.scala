package rpgboss.model.battle

import rpgboss.model._

case class Hit(hitActor: BattleStatus, damage: Damage, animationId: Int)

/**
 * A BattleAction is an action taken by an entity in the battle. It consumes
 * the initiative a.k.a. readiness of that entity and moves it to the back
 * of the ready queue.
 */
trait BattleAction {
  def actor: BattleStatus
  def targets: Array[BattleStatus]

  def process(battle: Battle): Array[Hit]
}

case class NullAction(actor: BattleStatus) extends BattleAction {
  override def targets = Array()
  def process(battle: Battle) = {
    Array()
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

    hits.toArray
  }
}

case class SkillAction(actor: BattleStatus, targets: Array[BattleStatus],
                       skillId: Int)
  extends BattleAction {
  def process(battle: Battle) = {
    assume(skillId < battle.pData.enums.skills.length)
    val skill = battle.pData.enums.skills(skillId)
    if (actor.mp < skill.cost)
      Array()

    actor.mp -= skill.cost

    assert(targets.length >= 1)

    val hits = new collection.mutable.ArrayBuffer[Hit]
    for (target <- targets) {
      hits ++= skill.applySkill(actor, target)
    }

    hits.toArray
  }
}

