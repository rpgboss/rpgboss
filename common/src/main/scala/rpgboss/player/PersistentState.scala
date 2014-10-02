package rpgboss.player

import scala.collection.mutable.{ HashMap => MutableHashMap }
import scala.collection.mutable.Publisher
import rpgboss.lib.ThreadChecked
import rpgboss.model.Project
import rpgboss.model.battle.PartyParameters
import rpgboss.model.MapLoc
import rpgboss.save.SaveFile
import rpgboss.save.SavedEventState

trait PersistentStateUpdate
case class IntChange(key: String, value: Int) extends PersistentStateUpdate
case class EventStateChange(key: (String, Int), value: Int)
  extends PersistentStateUpdate

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState(
  initial: SaveFile = SaveFile())
  extends ThreadChecked
  with Publisher[PersistentStateUpdate]
  with HasScriptConstants {

  private val intMap = new MutableHashMap[String, Int]
  intMap ++= initial.intMap

  private val intArrayMap = new MutableHashMap[String, Array[Int]]
  intArrayMap ++= initial.intArrayMap

  private val stringArrayMap = new MutableHashMap[String, Array[String]]
  stringArrayMap ++= initial.stringArrayMap

  private val mapLocMap = new MutableHashMap[String, MapLoc]
  mapLocMap ++= initial.mapLocMap

  // mapName->eventId->state
  private val eventStates = new MutableHashMap[(String, Int), Int]
  eventStates ++=
    initial.eventStates.map(es => (es.mapName, es.eventId) -> es.eventState)

  def toSerializable = {
    val serializedEventStates = eventStates.map {
      case ((mapName, eventId), eventState) =>
        SavedEventState(mapName, eventId, eventState)
    }
    new SaveFile(intMap.toMap, intArrayMap.toMap, stringArrayMap.toMap,
        mapLocMap.toMap, serializedEventStates.toArray)
  }


  def getLoc(key: String) = {
    mapLocMap.getOrElse(key, MapLoc())
  }

  def setLoc(key: String, loc: MapLoc) = {
    mapLocMap.update(key, loc)
  }

  // TODO: save player location
  def setInt(key: String, value: Int) = {
    assertOnBoundThread()
    intMap.update(key, value)
    publish(IntChange(key, value))
  }
  def getInt(key: String) = {
    assertOnBoundThread()
    intMap.get(key).getOrElse(0)
  }

  /**
   * Returns a defensive copy of the stored array.
   */
  def getIntArray(key: String) = {
    assertOnBoundThread()
    intArrayMap.get(key).map(_.clone()).getOrElse(new Array[Int](0))
  }

  def setIntArray(key: String, value: Array[Int]) = {
    assertOnBoundThread()
    intArrayMap.update(key, value.toArray)
  }

  /**
   * Returns a defensive copy of the stored array.
   */
  def getStringArray(key: String) = {
    assertOnBoundThread()
    stringArrayMap.get(key).map(_.clone()).getOrElse(new Array[String](0))
  }

  def setStringArray(key: String, value: Array[String]) = {
    assertOnBoundThread()
    stringArrayMap.update(key, value.toArray)
  }

  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, eventId: Int) = {
    assertOnBoundThread()
    eventStates.get((mapName, eventId)).getOrElse(0)
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = {
    assertOnBoundThread()
    eventStates.update((mapName, eventId), newState)
    publish(EventStateChange((mapName, eventId), newState))
  }

  def getPartyParameters(project: Project) = {
    val charactersIdxs =
      (0 until project.data.enums.characters.length).toArray
    PartyParameters(
      getIntArray(CHARACTER_LEVELS),
      getIntArray(CHARACTER_HPS),
      getIntArray(CHARACTER_MPS),
      charactersIdxs.map(id => getIntArray(CHARACTER_EQUIP(id))),
      charactersIdxs.map(id => getIntArray(CHARACTER_STATUS_EFFECTS(id))),
      getIntArray(CHARACTER_ROWS))
  }

  /**
   * @return  the list of characters that leveled up by their character index.
   */
  def givePartyExperience(
    characters: Array[rpgboss.model.Character],
    partyIds: Array[Int],
    experience: Int) = {
    val levels = getIntArray(CHARACTER_LEVELS)
    val exps = getIntArray(CHARACTER_EXPS)

    assert(levels.length == characters.length)
    assert(exps.length == characters.length)

    val leveledBuffer = collection.mutable.ArrayBuffer[Int]()
    for (i <- partyIds) {
      val character = characters(i)
      exps(i) += experience

      var leveled = false
      while (exps(i) >= character.expToLevel(levels(i))) {
        exps(i) -= character.expToLevel(levels(i))
        levels(i) += 1

        leveled = true
      }

      if (leveled) {
        character.expToLevel(levels(i))
        leveledBuffer += i
      }
    }

    setIntArray(CHARACTER_LEVELS, levels)
    setIntArray(CHARACTER_EXPS, exps)

    leveledBuffer.toArray
  }

  def saveCharacterVitals(
    characterId: Int, hp: Int, mp: Int, tempStatusEffects: Array[Int]) = {
    val hps = getIntArray(CHARACTER_HPS)
    val mps = getIntArray(CHARACTER_MPS)

    assert(characterId < hps.length)
    assert(characterId < mps.length)

    setIntArray(CHARACTER_HPS, hps.updated(characterId, hp))
    setIntArray(CHARACTER_MPS, mps.updated(characterId, mp))

    setIntArray(CHARACTER_STATUS_EFFECTS(characterId), tempStatusEffects)
  }

  /**
   * Adds or removes items. If we try to remove a greater quantity of an itemId
   * than exists in the inventory, nothing happens and we return false.
   * @return  Whether the items were successfully added or removed.
   */
  def addRemoveItem(itemId: Int, qtyDelta: Int): Boolean = {
    assert(qtyDelta != 0)

    val itemQtys = getIntArray(INVENTORY_QTYS)
    val itemIds = getIntArray(INVENTORY_ITEM_IDS)

    val idxOfItem = itemIds.indexOf(itemId)
    val itemExistsInInventory = idxOfItem != -1

    if (qtyDelta > 0) {
      if (itemExistsInInventory) {
        itemQtys(idxOfItem) += qtyDelta
        setIntArray(INVENTORY_QTYS, itemQtys)
      } else {
        setIntArray(INVENTORY_QTYS, itemQtys ++ Array(qtyDelta))
        setIntArray(INVENTORY_ITEM_IDS, itemIds ++ Array(itemId))
      }
    } else {
      if (!itemExistsInInventory)
        return false
      if (itemQtys(idxOfItem) < -qtyDelta)
        return false

      // Adding because qtyDelta is negative.
      itemQtys(idxOfItem) += qtyDelta

      // Trim items on the right side that have zero quantity. Also trim the
      // associated item ids.
      if (itemQtys(idxOfItem) == 0) {
        val newItemQtys = itemQtys.reverse.dropWhile(_ <= 0).reverse
        setIntArray(INVENTORY_QTYS, newItemQtys)
        setIntArray(INVENTORY_ITEM_IDS, itemIds.take(newItemQtys.size))
      } else {
        setIntArray(INVENTORY_QTYS, itemQtys)
        setIntArray(INVENTORY_ITEM_IDS, itemIds)
      }
    }

    return true
  }
}