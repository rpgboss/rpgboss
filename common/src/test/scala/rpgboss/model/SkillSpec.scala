package rpgboss.model

import rpgboss._
import rpgboss.model.battle._

class SkillSpec extends UnitSpec {
  import EffectKey._

  def fixture = new {
    val pData = ProjectData()

    val status = new BattleStatus(
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
  }

  "Skill" should "use RecoverHpAdd effects" in {
    val f = fixture
    f.status.hp = 20

    val skill = Skill(damages = Array(),
                      effects = Array(Effect(RecoverHpAdd.id, 20, 0)))
    val hits = skill.applySkill(f.status, f.status)
    hits.length should equal (1)
    hits.head.damages.length should equal (1)
    hits.head.damages.head.value should equal (-20)

    f.status.hp should equal (40)
  }
}