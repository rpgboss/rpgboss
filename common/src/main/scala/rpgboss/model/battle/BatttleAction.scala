package rpgboss.model.battle

import rpgboss.model._

trait BattleAction {
  def source: BattleStatus
  def process(battle: Battle)
}
 
case class NullAction(source: BattleStatus) extends BattleAction {
  def process(battle: Battle) = {}
}

/**
 * Indicates when this entity will be ready
 */
case class ReadyAction(source: BattleStatus) extends BattleAction {
  def process(battle: Battle) = {
    battle.readyQueue.enqueue(source)
  }
}

case class AttackAction(source: BattleStatus, target: BattleStatus)
  extends BattleAction {
  def process(battle: Battle) = {
    
  }
}

case class SkillAction(source: BattleStatus, target: BattleStatus)
  extends BattleAction {
  def process(battle: Battle) = {
    
  }
}

case class TimestampedBattleAction(time: Double, action: BattleAction)