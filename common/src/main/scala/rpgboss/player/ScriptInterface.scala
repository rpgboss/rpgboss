package rpgboss.player

import aurelienribon.tweenengine._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.battle._
import rpgboss.model.resource._
import rpgboss.player.entity._
import Predef._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.mozilla.javascript.NativeObject
import rpgboss.save.SaveFile
import rpgboss.save.SaveInfo
import rpgboss.model.event.EventJavascript

case class EntityInfo(x: Float, y: Float, dir: Int)

object EntityInfo {
  def apply(e: Entity): EntityInfo = apply(e.x, e.y, e.dir)
}

case class CurrentAndProposedStats(
  current: BattleStats, proposed: BattleStats)

trait HasScriptConstants {
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right

  val PLAYER_LOC = "playerLoc"

  val GOLD = "gold"
  val PLAYER_MOVEMENT_LOCKS = "playerMovementLocks"

  val PARTY = "party"
  val INVENTORY_ITEM_IDS = "inventoryIdxs"
  val INVENTORY_QTYS = "inventoryQtys"
  val CHARACTER_NAMES = "characterNames"
  val CHARACTER_LEVELS = "characterLevels"
  val CHARACTER_HPS = "characterHps"
  val CHARACTER_MPS = "characterMps"
  val CHARACTER_MAX_HPS = "characterMaxHps"
  val CHARACTER_MAX_MPS = "characterMaxMps"
  val CHARACTER_EXPS = "characterExps"
  val CHARACTER_ROWS = "characterRow"

  def CHARACTER_EQUIP(characterId: Int) =
    "characterEquip-%d".format(characterId)

  def CHARACTER_STATUS_EFFECTS(characterId: Int) =
    "characterStatusEffects-%d".format(characterId)

  val PICTURE_SLOTS = PictureSlots.END
}

/**
 * ScriptInterface is bound to a particular screen.
 *
 * These methods should be called only from scripting threads. Calling these
 * methods on the Gdx threads will likely cause deadlocks.
 *
 * @param   game    Allowed to be null so this may be used when there is only an
 *                  activeScreen.
 *
 * TODO: Eliminate the mapScreen argument. Map-related scripting commands should
 * probably not even be defined when there is no map.
 */
class ScriptInterface(
  game: RpgGame,
  activeScreen: RpgScreen)
  extends HasScriptConstants
  with ThreadChecked
  with LazyLogging {
  assume(activeScreen != null)

  private def mapScreen = game.mapScreen
  private def persistent = game.persistent
  def project = game.project

  def syncRun[T](op: => T): T = {
    assertOnDifferentThread()
    GdxUtils.syncRun(op)
  }

  /*
   * The below functions are all called from the script threads only.
   */

  /*
   * Accessors to various game data
   */
  def getMap(loc: MapLoc) =
    RpgMap.readFromDisk(project, loc.map)

  /*
   * Things to do with the player's location and camera
   */

  def teleport(mapName: String, x: Float, y: Float,
      transitionId: Int = Transitions.NONE.id) = {
    val loc = MapLoc(mapName, x, y)
    val map = getMap(loc)
    val transition = Transitions.get(transitionId)
    val fadeDuration = Transitions.fadeLength

    if (transition == Transitions.FADE) {
      syncRun {
        mapScreen.windowManager.setTransition(0, 1, fadeDuration)
      }
      sleep(fadeDuration);
    }

    syncRun {
      game.setPlayerLoc(loc);
    }

    if (transition == Transitions.FADE) {
      syncRun {
        mapScreen.windowManager.setTransition(1, 0, fadeDuration);
      }
    }
  }

  /**
   * Moves the map camera.
   */
  def moveCamera(dx: Float, dy: Float, async: Boolean) = {
    val move = syncRun { mapScreen.camera.enqueueMove(dx, dy) }
    if (!async)
      move.awaitFinish()
  }

  /**
   * Gets the position of the map camera.
   */
  def getCameraPos() = syncRun {
    mapScreen.camera.info
  }

  /*
   * Things to do with the screen
   */

  def setTransition(
    startAlpha: Float,
    endAlpha: Float,
    duration: Float) = syncRun {
    activeScreen.windowManager.setTransition(startAlpha, endAlpha, duration)
  }

  def startBattle(encounterId: Int, battleBackground: String) = {
    syncRun {
      game.startBattle(encounterId, battleBackground)
    }

    // Blocks until the battle screen finishes on way or the other
    game.battleScreen.finishChannel.read
  }

  def endBattleBackToMap() = {
    setTransition(0, 1, 0.5f)
    sleep(0.5f)

    syncRun {
      game.setScreen(game.mapScreen)

      // TODO fix hack of manipulating mapScreen directly
      game.mapScreen.windowManager.setTransition(1, 0, 0.5f)

      game.battleScreen.endBattle()
    }
  }

  def showPicture(slot: Int, name: String, rect: Rect) = syncRun {
    activeScreen.windowManager.showPictureByName(slot, name, rect)
  }

  def hidePicture(slot: Int) = syncRun {
    activeScreen.windowManager.hidePicture(slot)
  }

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
                loop: Boolean, fadeDuration: Float) = syncRun {
    activeScreen.playMusic(slot, specOpt, loop, fadeDuration)
  }

  /*
   * Things to do with user interaction
   */
  def sleep(duration: Float) = {
    Thread.sleep((duration * 1000).toInt)
  }

  /**
   * TODO: This is named different currently to allow newChoiceWindow to call
   * into this and use its default arguments. This should be renamed.
   */
  def newChoiceWindow(
    lines: Array[String],
    rect: Rect,
    options: NativeObject): ChoiceWindow#ChoiceWindowScriptInterface = {
    newChoiceWindow(
        lines, rect,
        JsonUtils.nativeObjectToCaseClass[TextChoiceWindowOptions](options))
  }

  def newChoiceWindow(
    lines: Array[String],
    rect: Rect,
    options: TextChoiceWindowOptions): ChoiceWindow#ChoiceWindowScriptInterface = {
    val window = syncRun {
      new TextChoiceWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        lines,
        rect,
        options)
    }

    window.scriptInterface
  }

  def newStaticTextWindow(
    lines: Array[String],
    rect: Rect,
    options: NativeObject): TextWindow#TextWindowScriptInterface = {
    newStaticTextWindow(
        lines, rect,
        JsonUtils.nativeObjectToCaseClass[TextWindowOptions](options))
  }

  def newStaticTextWindow(
    lines: Array[String],
    rect: Rect,
    options: TextWindowOptions): TextWindow#TextWindowScriptInterface = {
    val window = syncRun {
      new TextWindow(
          game.persistent,
          activeScreen.windowManager,
          activeScreen.inputs,
          lines,
          rect,
          options)
    }

    window.scriptInterface
  }

  /**
   * Choices are arrays of [x, y, w, h] in screen coordinates. Returns either
   * the choice index, or -1 if the choices were invalid.
   */
  def getSpatialChoice(choices: Array[Array[Int]], defaultChoice: Int): Int = {
    if (choices.length == 0)
      return -1

    for (choice <- choices) {
      if (choice.length != 4)
        return -1
      if (choice(2) <= 0 || choice(3) <= 0)
        return -1
    }

    getSpatialChoice(
      choices.map(x => Set(Rect(x(0), x(1), x(2), x(3)))), defaultChoice)
  }

  /**
   * TODO: No idea how this would be called from Javascript, but it's convenient
   * from Scala.
   */
  def getSpatialChoice(choices: Array[Set[Rect]],
                       defaultChoice: Int): Int = {
    assert(!choices.isEmpty)

    val window = syncRun {
      new SpatialChoiceWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        choices,
        defaultChoice = defaultChoice)
    }

    val choice = window.scriptInterface.getChoice()
    window.scriptInterface.close()
    choice
  }

  def showText(text: Array[String], rect: Rect, timePerChar: Float) = {
    val window = syncRun {
      new PrintingTextWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        text,
        rect,
        timePerChar)
    }
    window.scriptInterface.awaitClose()
  }

  def showText(text: Array[String]): Unit =
    showText(
      text,
      activeScreen.layout.south(640, 180),
      timePerChar = 0.02f)

  def getPlayerEntityInfo(): EntityInfo = syncRun {
    mapScreen.getPlayerEntityInfo()
  }

  def getEventEntityInfo(id: Int): Option[EntityInfo] = {
    mapScreen.eventEntities.get(id).map(EntityInfo.apply)
  }

  def activateEvent(id: Int, awaitFinish: Boolean) = {
    val eventOpt = mapScreen.eventEntities.get(id)

    if (eventOpt.isEmpty)
      logger.error("Could not activate event id: %d".format(id))

    val scriptOpt = eventOpt.flatMap(_.activate(SpriteSpec.Directions.NONE))

    if (awaitFinish)
      scriptOpt.map(_.awaitFinish())

    scriptOpt.isDefined
  }

  def moveEvent(id: Int, dx: Float, dy: Float,
                affixDirection: Boolean = false,
                async: Boolean = false) = {
    val move = mapScreen.moveEvent(id, dx, dy, affixDirection)
    if (move != null && !async)
      move.awaitFinish()
  }

  def movePlayer(dx: Float, dy: Float,
                 affixDirection: Boolean = false,
                 async: Boolean = false) = {
    val move = mapScreen.movePlayer(dx, dy, affixDirection)
    if (move != null && !async)
      move.awaitFinish()
  }

  def getEventState(mapName: String, eventId: Int) = syncRun {
    persistent.getEventState(mapName, eventId)
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = syncRun {
    persistent.setEventState(mapName, eventId, newState)
  }

  def incrementEventState(eventId: Int) = syncRun {
    mapScreen.mapName.map { mapName =>
      val newState = persistent.getEventState(mapName, eventId) + 1
      persistent.setEventState(mapName, eventId, newState)
    }
  }

  def modifyParty(add: Boolean, characterId: Int): Boolean = {
    // Can't be anonymous due to use of 'return', which breaks out of closures.
    def f(): Boolean = {
      if (characterId >= project.data.enums.characters.size)
        return false

      val existing = persistent.getIntArray(PARTY)
      if (add) {
        if (existing.contains(characterId))
          return false

        val newParty = existing :+ characterId
        persistent.setIntArray(PARTY, newParty)
        return true
      } else {
        if (!existing.contains(characterId))
          return false

        persistent.setIntArray(PARTY, existing.filter(_ != characterId))
        return true
      }
    }

    syncRun { f() }
  }

  def openStore(itemIdsSold: Array[Int], buyPriceMultiplier: Float,
                sellPriceMultiplier: Float) = {
    assert(activeScreen == game.mapScreen)
    val finishable = syncRun {
      val statement = EventJavascript.jsStatement(
          "openStore", itemIdsSold, buyPriceMultiplier, sellPriceMultiplier)
      println(statement)
      game.mapScreen.scriptFactory.runFromFile(
        "sys/store.js",
        statement,
        None)
    }
    finishable.awaitFinish()
  }

  def addRemoveItem(itemId: Int, qtyDelta: Int) = syncRun {
    persistent.addRemoveItem(itemId, qtyDelta)
  }

  def addRemoveGold(delta: Int) = syncRun {
    persistent.addRemoveGold(delta)
  }

  def useItemInMenu(itemId: Int, characterId: Int) = syncRun {
    if (persistent.addRemoveItem(itemId, -1)) {
      val item = project.data.enums.items(itemId)
      val characterStatus = BattleStatus.fromCharacter(
          project.data, persistent.getPartyParameters(project), characterId,
          index = -1)

      val damages = item.effects.flatMap(_.applyAsSkillOrItem(characterStatus))

      for (damage <- damages) {
        logger.debug("Character %d took %d damage from item.".format(
            characterId, damage.value))
      }

      characterStatus.clampVitals()

      persistent.saveCharacterVitals(characterId, characterStatus.hp,
          characterStatus.mp, characterStatus.tempStatusEffects)
    }
  }

  def getBattleStats(characterId: Int, proposedSlotId: Int,
      proposedItemId: Int) = {
    val partyParams = syncRun { persistent.getPartyParameters(project) }
    val currentBattleStats = BattleStatus.fromCharacter(
        project.data, partyParams, characterId)

    if (proposedSlotId > 0 && proposedItemId > 0) {
      partyParams.characterEquip(characterId).update(
          proposedSlotId, proposedItemId)
    }

    val proposedBattleStats =
      BattleStatus.fromCharacter(project.data, partyParams, characterId)

    CurrentAndProposedStats(currentBattleStats.stats, proposedBattleStats.stats)
  }

  def getInt(key: String): Int = syncRun {
    persistent.getInt(key)
  }
  def setInt(key: String, value: Int) = syncRun {
    persistent.setInt(key, value)
  }
  def getIntArray(key: String): Array[Int] = syncRun {
    persistent.getIntArray(key)
  }
  def setIntArray(key: String, value: Array[Int]) = syncRun {
    persistent.setIntArray(key, value)
  }
  def getStringArray(key: String): Array[String] = syncRun {
    persistent.getStringArray(key)
  }
  def setStringArray(key: String, value: Array[String]) = syncRun {
    persistent.setStringArray(key, value)
  }

  def getEquippableItems(characterId: Int, equipTypeId: Int) = syncRun {
    persistent.getEquippableItems(project.data, characterId, equipTypeId)
  }

  def equipItem(characterId: Int, slotId: Int, itemId: Int) = syncRun {
    persistent.equipItem(characterId, slotId, itemId)
  }

  def startNewGame() = syncRun {
    game.startNewGame()
  }

  def getSaveInfos(maxSlots: Int): Array[SaveInfo] = {
    val seq = for (slot <- 0 until maxSlots) yield {
      SaveFile.readInfo(project, slot)
    }
    seq.toArray
  }

  def loadFromSaveSlot(slot: Int) = syncRun {
    game.loadGame(slot)
  }

  def saveToSaveSlot(slot: Int) = syncRun {
    game.saveGame(slot)
  }

  def quit() = syncRun {
    game.quit()
  }

  /**
   * Should be used for testing only
   */
  def mapScreenKeyPress(key: Int, duration: Float) = {
    GdxUtils.syncRun { mapScreen.inputs.myKeyDown(key) }
    sleep(duration)
    GdxUtils.syncRun { mapScreen.inputs.myKeyUp(key) }
  }

  def mapScreenKeyPress(key: Int): Unit = {
    mapScreenKeyPress(key, 0.1f)
  }
}

object ScriptInterfaceConstants extends HasScriptConstants {

}