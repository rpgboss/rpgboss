package rpgboss.model.battle

import rpgboss.model._

/**
 * A BattleAction is an action taken by an entity in the battle. It consumes 
 * the initiative a.k.a. readiness of that entity and moves it to the back
 * of the ready queue.
 */
trait BattleAction {
  def actor: BattleStatus
  def target: BattleStatus
  def process(battle: Battle): Array[TakenDamage]
}

case class NullAction(actor: BattleStatus) extends BattleAction {
  override def target = null
  def process(battle: Battle) = {
    Array()
  }
}

case class AttackAction(actor: BattleStatus, target: BattleStatus)
  extends BattleAction {
  def process(battle: Battle) = {
    val damages = 
      actor.onAttackSkillIds
        .map(skillId => Damage.getDamages(actor, target, battle.pData, skillId))
        .flatten
    
    target.hp -= math.min(target.hp, damages.map(_.value).sum)
    
    damages
  }
}

case class SkillAction(actor: BattleStatus, target: BattleStatus, skillId: Int)
  extends BattleAction {
  def process(battle: Battle) = {
    if (skillId < battle.pData.enums.skills.length)
      Array()
    
    val skill = battle.pData.enums.skills(skillId)
    if (actor.mp < skill.cost)
      Array()
      
    actor.mp -= skill.cost
      
    val damages = Damage.getDamages(actor, target, battle.pData, skillId)
    target.hp -= math.min(target.hp, damages.map(_.value).sum)
    
    damages
  }
}

