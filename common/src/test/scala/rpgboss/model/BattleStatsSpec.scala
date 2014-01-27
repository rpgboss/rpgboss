package rpgboss.model

import rpgboss._
import com.google.common.io.Files

class BattleStatsSpec extends UnitSpec {
  def fixture = new {
    val pData = ProjectData("fake-uuid", "fake-title")

    val character = {
      val statProgressions = StatProgressions(
        exp = Curve(300, 100),
        mhp = Curve(50, 10),
        mmp = Curve(20, 4),
        atk = Curve(10, 2),
        spd = Curve(10, 2),
        mag = Curve(10, 2),
        arm = Curve(10, 1),
        mre = Curve(10, 1))

      Character(progressions = statProgressions)
    }

    val baseStats = BaseStats(
      mhp = 50,
      mmp = 20,
      atk = 10,
      spd = 10,
      mag = 10,
      arm = 10,
      mre = 10,
      effects = Seq())
  }

  "BattleStats" should "work with character stat progressions" in {
    val f = fixture

    val stats1 = BattleStats(f.pData, f.character.baseStats(f.pData, 1))
    stats1 should equal (BattleStats(50, 20, 10, 10, 10, 10, 10, Seq()))

    val stats11 = BattleStats(f.pData, f.character.baseStats(f.pData, 11))
    stats11 should equal (BattleStats(150, 60, 30, 30, 30, 20, 20, Seq()))
  }

  "BattleStats" should "work with status effects" in {
    val f = fixture
    f.pData.enums.statusEffects = Seq(StatusEffect(
      effects = Seq(Effect(EffectKey.AtkAdd.id, 10, 0))))

    val stats1 =
      BattleStats(f.pData, f.baseStats, tempStatusEffectIds = Seq(0))
    stats1 should equal (BattleStats(
      50, 20, 20, 10, 10, 10, 10, Seq(f.pData.enums.statusEffects(0))))
  }

  "BattleStats" should "work with equipment effects" in {
    val f = fixture
    f.pData.enums.items =
      Seq(Item(effects = Seq(Effect(EffectKey.AtkAdd.id, 10, 0))))

    val stats1 = BattleStats(f.pData, f.baseStats, equippedIds = Seq(0))
    stats1 should equal (BattleStats(50, 20, 20, 10, 10, 10, 10, Seq()))
  }

  "BattleStats" should "work with equipment status effects" in {
    val f = fixture
    f.pData.enums.statusEffects = Seq(StatusEffect(
      effects = Seq(Effect(EffectKey.AtkAdd.id, 10, 0))))
    f.pData.enums.items =
      Seq(Item(effects = Seq(Effect(EffectKey.AddStatusEffect.id, 0, 0))))

    val stats1 = BattleStats(f.pData, f.baseStats, equippedIds = Seq(0))
    stats1 should equal (BattleStats(
      50, 20, 20, 10, 10, 10, 10, Seq(f.pData.enums.statusEffects(0))))
  }


}