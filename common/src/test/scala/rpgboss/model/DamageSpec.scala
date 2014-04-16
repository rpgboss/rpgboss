package rpgboss.model

import rpgboss._
import rpgboss.model.battle._

class DamageSpec extends UnitSpec {
  def fixture = new {
    val pData = ProjectData()
    def status = new BattleStatus(
      pData,
      BattleEntityType.Party,
      0,
      50,
      20,
      BaseStats(50, 20, 10, 10, 10, 10, 10, Seq()),
      Seq(),
      Seq(),
      0)
  }

  "Damage" should "evaluate ATK and HP in the formula" in {
    val f = fixture

    val status = f.status
    status.stats should equal (
      BattleStats(50, 20, 10, 10, 10, 10, 10, 
                  f.pData.enums.elements.map(v => 0), Seq()))

    val damage = Damage(formula = "a.atk")
    damage.getBaseDamage(status, status) should equal(10)
  }
}