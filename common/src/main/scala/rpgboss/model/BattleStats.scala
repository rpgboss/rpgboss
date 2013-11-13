package rpgboss.model

case class Stat(name: String) extends HasName

case class Curve(base: Int, perLevel: Int) {
  def apply(x: Int) = {
    perLevel * (x - 1) + base
  }
}

case class StatProgressions(
  exp: Curve = Curve(300, 100), // Exp required to level up. Not cumulative.
  mhp: Curve = Curve(50, 10),
  mmp: Curve = Curve(20, 4),
  atk: Curve = Curve(10, 2),
  spd: Curve = Curve(10, 2),
  mag: Curve = Curve(10, 2))

case class StatusEffect(
  name: String = "",
  effects: Seq[Effect] = Seq(),
  releaseOnBattleEnd: Boolean = false,
  releaseTime: Int = 0,
  releaseChance: Int = 0,
  releaseDmgChance: Int = 0,
  maxStacks: Int = 1) extends HasName

case class BattleStats(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  statusEffects: Seq[StatusEffect])
  
object BattleStats {
  def apply(pData: ProjectData, characterId: Int, level: Int, 
            equippedIds: Seq[Int] = Seq(),
            otherStatusEffectIds: Seq[Int] = Seq()): BattleStats = {
    require(characterId >= 0 && characterId < pData.enums.characters.length)
    require(equippedIds.forall(i => i >= 0 && i < pData.enums.items.length))
    require(otherStatusEffectIds.forall(
      i => i >= 0 && i < pData.enums.statusEffects.length))
    
    val character = pData.enums.characters(characterId)
      
    val equipment = equippedIds.map(pData.enums.items)
    val equipmentEffects = equipment.flatMap(_.effects)
    
    // Make sure we don't apply more than max-stacks of each status effect
    val stackedStatusEffects: Seq[StatusEffect] = {
      val statusEffectStackMap = collection.mutable.Map[Int, Int]()
      val statusEffectBuffer = collection.mutable.ArrayBuffer[StatusEffect]()
      
      val equipmentStatusEffectIds = 
        equipmentEffects
          .filter(_.keyId == EffectKey.AddStatusEffect.id)
          .map(_.v)
          .filter(_ >= 0)
          .filter(_ < pData.enums.statusEffects.length)
          
      for (statusEffectId <- equipmentStatusEffectIds ++ otherStatusEffectIds) {
        val statusEffect = pData.enums.statusEffects(statusEffectId)
        val currentCount = 
          statusEffectStackMap.getOrElseUpdate(statusEffectId, 0)
        
        if (currentCount < statusEffect.maxStacks) {
          statusEffectStackMap.update(statusEffectId, currentCount + 1)
          statusEffectBuffer.append(statusEffect)
        }
      }
      
      statusEffectBuffer.toList
    }
    
    val allEffects = equipmentEffects ++ stackedStatusEffects.flatMap(_.effects)
      
    def addEffects(key: EffectKey.Value): Int = {
      allEffects.filter(_.keyId == key.id).map(_.v).sum
    }
    
    apply(
      mhp = character.progressions.mhp(level) + addEffects(EffectKey.MhpAdd),
      mmp = character.progressions.mmp(level) + addEffects(EffectKey.MmpAdd),
      atk = character.progressions.atk(level) + addEffects(EffectKey.AtkAdd),
      spd = character.progressions.spd(level) + addEffects(EffectKey.SpdAdd),
      mag = character.progressions.mag(level) + addEffects(EffectKey.MagAdd),
      statusEffects = stackedStatusEffects
    )
  }
}
  
  