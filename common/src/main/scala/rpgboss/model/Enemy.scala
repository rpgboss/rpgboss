package rpgboss.model

case class ItemDrop(var itemId: Int = 0, var chance: Float = 0.1f)

case class Enemy(
  var name: String = "",
  var battler: Option[BattlerSpec] = None,
  var level: Int = 5,
  var mhp: Int = 40,
  var mmp: Int = 40,
  var atk: Int = 10,
  var spd: Int = 10,
  var mag: Int = 10,
  var arm: Int = 10,
  var mre: Int = 10,

  var expValue: Int = 100,
  var droppedGold: Int = 25,
  var droppedItems: Array[ItemDrop] = Array(),

  var attackSkillId: Int = 0,

  var effects: Array[Effect] = Array(),
  var skillIds: Array[Int] = Array()) extends HasName {
  def baseStats =
    BaseStats(
      mhp = mhp,
      mmp = mmp,
      atk = atk,
      spd = spd,
      mag = mag,
      arm = arm,
      mre = mre,
      effects = effects)
}

/**
 * @param   x     Defines x-center of unit.
 */
case class EncounterUnit(
  enemyIdx: Int,
  var x: Int,
  var y: Int)

case class Encounter(
  var name: String = "",
  var units: Array[EncounterUnit] = Array(),
  var canEscape: Boolean = true,
  var escapeChance: Float = 0.9f) extends HasName

object Encounter {
  def getEnemyLabels(
    units: Array[EncounterUnit],
    pData: ProjectData): Array[String] = {
    val enemyLabels = new collection.mutable.ArrayBuffer[String]

    // Array of same length and enemies to keep track of how many there are
    val enemyCounts = Array.fill(pData.enums.enemies.length)(0)
    for (unit <- units; if unit.enemyIdx < enemyCounts.length) {
      enemyCounts(unit.enemyIdx) += 1
    }

    for (i <- 0 until enemyCounts.length) {
      val count = enemyCounts(i)

      if (count > 0) {
        val enemyName = pData.enums.enemies(i).name
        if (count == 1)
          enemyLabels.append(enemyName)
        else
          enemyLabels.append("%d x %s".format(count, enemyName))
      }
    }

    enemyLabels.toArray
  }
}