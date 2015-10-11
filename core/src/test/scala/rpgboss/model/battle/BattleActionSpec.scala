package rpgboss.model.battle

import rpgboss._
import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.player.PersistentState

class BattleActionSpec extends UnitSpec {
  def fixture() = {
    val f = new BattleTest.BattleFixture() {
      val persistent = new PersistentState()
      persistent.addRemoveItem(0, 10)
      persistent.addRemoveItem(1, 10)
      val fastCharStatus = battle.partyStatus.head
    }
    val dmg20 =
    f.pData.enums.skills = Array(
        Skill(effects = Array(Effect(RecoverHpAdd.id, 20))))
    f.pData.enums.items = Array(
        Item(effects = Array(Effect(RecoverHpAdd.id, 30))),
        Item(useOnAttack = true, equippedAttackSkillId = 0))
    f
  }

  "ItemAction" should "work with direct item effects" in {
    val f = fixture()

    f.fastCharStatus.hp should equal(1)

    val action =
      ItemAction(f.fastCharStatus, Array(f.fastCharStatus), 0, f.persistent)
    action.process(f.battle)

    f.fastCharStatus.hp should equal(31)

  }
}