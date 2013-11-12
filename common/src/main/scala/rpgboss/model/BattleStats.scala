package rpgboss.model

case class Stat(name: String) extends HasName

case class CharProgressions(
  exp: Curve = Curve(300, 100), // Exp required to level up. Not cumulative.
  mhp: Curve = Curve(50, 10),
  mmp: Curve = Curve(20, 4),
  atk: Curve = Curve(10, 2),
  spd: Curve = Curve(10, 2),
  mag: Curve = Curve(10, 2))

case class Effect(key: String, v: Int)

case class StatusEffect(
  name: String = "",
  effects: Seq[Effect] = Seq(),
  releaseOnBattleEnd: Boolean = false,
  releaseTime: Int = 0,
  releaseChance: Int = 0,
  releaseDmgChance: Int = 0,
  maxStacks: Int = 1) extends HasName

case class BattleStatsPermanent(
  mhp: Int,
  mmp: Int,
  atk: Int,
  spd: Int,
  mag: Int,
  statusEffects: Seq[StatusEffect])
  
object BattleStatsPermanent {
  def apply(allEquipment: Seq[Item], character: Character, level: Int, 
            equippedIds: Seq[Int]): BattleStatsPermanent = {
    require(equippedIds.forall(i => i >= 0 && i < allEquipment.length))

    val equipment = equippedIds.map(allEquipment)
    val allEffects = equipment.flatMap(_.effects)
    
    
    
    apply(
      mhp = character.progressions.mhp(level),
      mmp = character.progressions.mmp(level),
      atk = character.progressions.atk(level),
      spd = character.progressions.spd(level),
      mag = character.progressions.mag(level)
      )
  }
}
  
  