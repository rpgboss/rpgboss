package rpgboss.model

import rpgboss._
import rpgboss.model.battle._

class DamageSpec extends UnitSpec {
  def fixture = new {
    val pData = ProjectData()

    def status = new BattleStatus(
      0,
      pData,
      BattleEntityType.Party,
      0,
      50,
      20,
      BaseStats(50, 20, 10, 10, 10, arm = 100, mre = 200, effects = Array()),
      Array(),
      Array(),
      Array(),
      Array(0),
      0)

    def testFormula(formula: String, expected: Int) = {
      val d = Damage(formula = formula)
      d.getBaseDamage(status, status) should equal(expected)
    }
  }

  "Damage" should "evaluate single terms" in {
    val f = fixture

    f.testFormula("a.hp", 50)
    f.testFormula("a.mp", 20)
    f.testFormula("a.mhp", 50)
    f.testFormula("a.mmp", 20)
    f.testFormula("a.atk", 10)
    f.testFormula("a.spd", 10)
    f.testFormula("a.mag", 10)
    f.testFormula("a.arm", 100)
    f.testFormula("a.mre", 200)
  }

  "Damage" should "do basic arithmetic" in {
    val f = fixture

    f.testFormula("a.hp + 10", 60)
    f.testFormula("a.atk / 2", 5)
  }

  "Damage" should "allow arithmetic between terms" in {
    val f = fixture

    f.testFormula("a.hp + 10 * a.atk", 150)
  }

  "Damage" should "fail gracefully and return a sentinal value" in {
    val f = fixture

    f.testFormula("foo", -1)
  }

  "Damage" should "work correctly in a skill, handling armor, magic res." in {
    val f = fixture

    f.pData.enums.elements = Array("Blunt", "Fire")
    f.pData.enums.skills = Array(Skill(damages = Array(
      Damage(DamageType.Physical.id, 0, "a.atk"),
      Damage(DamageType.Magic.id, 1, "a.mag"))))

    val takenDamages = Damage.getDamages(f.status, f.status, f.pData, 0)

    takenDamages.length should equal(2)
    takenDamages(0) should equal(TakenDamage(DamageType.Physical, 0, 5))
    takenDamages(1) should equal(TakenDamage(DamageType.Magic, 1, 3))
  }
}