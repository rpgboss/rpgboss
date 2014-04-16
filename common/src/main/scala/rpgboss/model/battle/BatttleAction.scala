package rpgboss.model.battle

import rpgboss.model._

trait BattleAction {
  def source: BattleStatus
  def process(battle: Battle)
}

case class TakenDamage(damageType: DamageType.Value, elementId: Int, value: Int)

object BattleAction {
  def getDamages(source: BattleStatus, target: BattleStatus, pData: ProjectData, 
                 skillId: Int): Seq[TakenDamage] = {
    import DamageType._
    
    assume(skillId < pData.enums.skills.length)
    val skill = pData.enums.skills(skillId)
    
    for (damage <- skill.damages) yield {
      val armorOrMagicResist = 
        if (damage.typeId == Physical.id) target.stats.arm else target.stats.mre
      
      val elementResist =
        if (damage.elementId < target.stats.elementResists.length)
          target.stats.elementResists(damage.elementId)
        else 
          0
      
      val totalResist = armorOrMagicResist + elementResist
      
      val resistMultiplier = 1.0 / (1.0 + (totalResist.toDouble / 100.0))
      
      TakenDamage(DamageType.apply(damage.typeId), damage.elementId, 0)
    }
  }
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
