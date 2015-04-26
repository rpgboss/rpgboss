package rpgboss.player

import rpgboss.UnitSpec
import rpgboss.model._

class PersistentStateSpec extends UnitSpec with HasScriptConstants {
  def levelingFixture(level1: Int, level2: Int, exp1: Int, exp2: Int) = new {
    val character1 =
      Character(progressions = StatProgressions(exp = Curve(300, 100)))
    val character2 =
      Character(progressions = StatProgressions(exp = Curve(600, 200)))
    val characters = Array(character1, character2)
    val pData = ProjectData()
    pData.enums.characters = characters

    val persistent = new PersistentState
    persistent.setIntArray(CHARACTER_ROWS, Array(0, 0))

    persistent.setIntArray(CHARACTER_HPS, Array(50, 40))
    persistent.setIntArray(CHARACTER_MPS, Array(15, 20))
    persistent.setIntArray(CHARACTER_LEVELS , Array(level1, level2))
    persistent.setIntArray(CHARACTER_EXPS, Array(exp1, exp2))

    def getState() = new {
      val hps = persistent.getIntArray(CHARACTER_HPS)
      val mps = persistent.getIntArray(CHARACTER_MPS)
      val levels = persistent.getIntArray(CHARACTER_LEVELS)
      val exps = persistent.getIntArray(CHARACTER_EXPS)
    }
  }

  "PersistentState" should "level up specified character only" in {
    val f = levelingFixture(1, 1, 0, 0)
    val leveled = f.persistent.giveExperience(f.pData, Array(1), 700)
    val state = f.getState()

    leveled should deepEqual (Array(1))
    state.hps should deepEqual(Array(50, 48))
    state.mps should deepEqual(Array(15, 24))
    state.levels should deepEqual (Array(1, 2))
    state.exps should deepEqual (Array(0, 100))
  }

  "PersistentState" should "level up both characters" in {
    val f = levelingFixture(1, 1, 0, 0)
    val leveled =
      f.persistent.giveExperience(f.pData, Array(0, 1), 600)
    val state = f.getState()

    leveled should deepEqual (Array(0, 1))
    state.hps should deepEqual(Array(60, 48))
    state.mps should deepEqual(Array(18, 24))
    state.levels should deepEqual (Array(2, 2))
    state.exps should deepEqual (Array(300, 0))
  }

  "PersistentState" should "level up through multiple levels" in {
    val f = levelingFixture(1, 1, 0, 0)
    val leveled =
      f.persistent.giveExperience(f.pData, Array(0, 1), 700)
    val state = f.getState()

    leveled should deepEqual (Array(0, 1))
    state.hps should deepEqual(Array(70, 48))
    state.mps should deepEqual(Array(21, 24))
    state.levels should deepEqual (Array(3, 2))
    state.exps should deepEqual (Array(0, 100))
  }

  "PersistentState" should "handle added and removed items" in {
    val p = new PersistentState() {
      def getZippedItems() = {
        val itemIds = getIntArray(INVENTORY_ITEM_IDS)
        val itemQtys = getIntArray(INVENTORY_QTYS)
        assert(itemIds.length == itemQtys.length)
        itemIds zip itemQtys
      }
    }

    p.getZippedItems().length should equal (0)
    p.addRemoveItem(0, -1) should equal (false)

    // Add 3 of item 2.
    p.addRemoveItem(2, 3) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->3))

    // Add 3 of item 2 again. Test that they sum correctly.
    p.addRemoveItem(2, 3) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->6))

    // Add 4 of item 3.
    p.addRemoveItem(3, 4) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->6, 3->4))

    // Try to remove more than exists of an item.
    p.addRemoveItem(2, -10) should equal (false)
    p.getZippedItems() should deepEqual(Array(2->6, 3->4))

    // Remove some of item 2.
    p.addRemoveItem(2, -2) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->4, 3->4))

    // Remove all of item 3.
    p.addRemoveItem(3, -4) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->4))

    // Add back some of item 3.
    p.addRemoveItem(3, 4) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->4, 3->4))

    // Remove all of item 2.
    p.addRemoveItem(2, -4) should equal (true)
    p.getZippedItems() should deepEqual(Array(2->0, 3->4))

    // Remove all of item 3.
    p.addRemoveItem(3, -4) should equal (true)
    p.getZippedItems() should deepEqual(Array[(Int, Int)]())
  }

  "PersistentState" should "get give a list of equippable items" in {
    val p = new PersistentState

    val pData = ProjectData()

    pData.enums.items = Array(
      Item(name = "Some Consumable", itemTypeId = ItemType.Consumable.id),
      Item(name = "Weapon1", itemTypeId = ItemType.Equipment.id, equipType = 0),
      Item(name = "Armor", itemTypeId = ItemType.Equipment.id, equipType = 1),
      Item(name = "Weapon2", itemTypeId = ItemType.Equipment.id, equipType = 0),
      Item(name = "NoHave", itemTypeId = ItemType.Equipment.id, equipType = 0))

    pData.enums.classes = Array(
      CharClass(canUseItems = Array(0, 1, 2, 3, 4)),
      CharClass(canUseItems = Array(1)),
      CharClass(canUseItems = Array(2, 3, 4)))

    pData.enums.characters = Array(
      Character(charClass = 0),
      Character(charClass = 1),
      Character(charClass = 2))

    p.addRemoveItem(0, 1)
    p.addRemoveItem(1, 2)
    p.addRemoveItem(2, 2)
    p.addRemoveItem(3, 2)

    p.getEquippableItems(pData, 0, 0) should deepEqual(Array(1, 3))
    p.getEquippableItems(pData, 0, 1) should deepEqual(Array(2))
    p.getEquippableItems(pData, 1, 0) should deepEqual(Array(1))
    p.getEquippableItems(pData, 1, 1) should deepEqual(Array[Int]())
    p.getEquippableItems(pData, 2, 0) should deepEqual(Array(3))
    p.getEquippableItems(pData, 2, 1) should deepEqual(Array(2))
  }

  "PersistentState" should "equip items correctly" in {
    val p = new PersistentState

    val pData = ProjectData()

    pData.enums.items = Array(
      Item(name = "Weapon1", itemTypeId = ItemType.Equipment.id, equipType = 0),
      Item(name = "Weapon2", itemTypeId = ItemType.Equipment.id, equipType = 0))

    p.addRemoveItem(0, 1)

    p.countItems(0) should equal (1)
    p.countItems(1) should equal (0)

    // Can't equip item id 1, because there are none.
    p.equipItem(0, 2, 1) should equal (false)

    p.equipItem(0, 2, 0) should equal (true)

    p.countItems(0) should equal (0)
    p.countItems(1) should equal (0)

    p.getIntArray(CHARACTER_EQUIP(0)) should deepEqual(Array(-1, -1, 0))

    p.equipItem(0, 2, -1) should equal (true)

    p.countItems(0) should equal (1)
    p.countItems(1) should equal (0)

    p.getIntArray(CHARACTER_EQUIP(0)) should deepEqual(Array(-1, -1, -1))
  }

  "PersistentState" should "add and remove gold correctly" in {
    val p = new PersistentState
    p.addRemoveGold(-1) should equal (false)
    p.addRemoveGold(20) should equal (true)
    p.addRemoveGold(-21) should equal (false)
    p.addRemoveGold(-20) should equal (true)
    p.addRemoveGold(-1) should equal (false)
  }

  "PersistentState" should "add and remove learned skills correctly" in {
    val p = new PersistentState
    p.getIntArray(CHARACTER_LEARNED_SKILLS(0)) should deepEqual(Array[Int]())
    p.getIntArray(CHARACTER_LEARNED_SKILLS(1)) should deepEqual(Array[Int]())

    p.addRemoveLearnedSkills(true, 0, 13)
    p.addRemoveLearnedSkills(true, 0, 12)
    p.addRemoveLearnedSkills(true, 1, 0)

    p.getIntArray(CHARACTER_LEARNED_SKILLS(0)) should deepEqual(
        Array[Int](13, 12))
    p.getIntArray(CHARACTER_LEARNED_SKILLS(1)) should deepEqual(Array[Int](0))

    p.addRemoveLearnedSkills(false, 0, 0)
    p.addRemoveLearnedSkills(false, 1, 0)

    p.getIntArray(CHARACTER_LEARNED_SKILLS(0)) should deepEqual(
        Array[Int](13, 12))
    p.getIntArray(CHARACTER_LEARNED_SKILLS(1)) should deepEqual(Array[Int]())
  }
}