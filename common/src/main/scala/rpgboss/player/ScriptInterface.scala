package rpgboss.player

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
import com.badlogic.gdx.graphics.Color

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Color

import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

case class EntityInfo(x: Float, y: Float, dir: Int,
    screenX: Float, screenY: Float)

object EntityInfo {
  def apply(e: Entity, mapScreen: MapScreen): EntityInfo = {
    val pxPerTileX = mapScreen.screenW / mapScreen.screenWTiles
    val pxPerTileY = mapScreen.screenH / mapScreen.screenHTiles
    val screenX =
      (e.x - mapScreen.camera.x) * pxPerTileX + (mapScreen.screenW / 2)
    val screenY =
      (e.y - mapScreen.camera.y) * pxPerTileY + (mapScreen.screenH / 2)
    apply(e.x, e.y, e.dir, screenX, screenY)
  }
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
  val CHARACTER_EXPS = "characterExps"
  val CHARACTER_ROWS = "characterRow"

  def CHARACTER_EQUIP(characterId: Int) =
    "characterEquip-%d".format(characterId)

  def CHARACTER_STATUS_EFFECTS(characterId: Int) =
    "characterStatusEffects-%d".format(characterId)

  val PICTURE_SLOTS = PictureSlots.END

  // Synchronized with LayoutType RpgEnum.
  val CENTERED = 0
  val NORTH = 1
  val EAST = 2
  val SOUTH = 3
  val WEST = 4
  val NORTHEAST = 5
  val SOUTHEAST = 6
  val SOUTHWEST = 7
  val NORTHWEST = 8

  // Synchronized with SizeType RpgEnum.
  val FIXED = 0
  val SCALE_SOURCE = 1
  val SCREEN = 2
  val COVER = 3
  val CONTAIN = 4
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
    if (onBoundThread())
      op
    else
      GdxUtils.syncRun(op)
  }

  /*
   * The below functions are all called from the script threads only.
   */

  /*
   * Accessors to various game data
   */
  def getScreenH() = project.data.startup.screenH
  def getScreenW() = project.data.startup.screenW

  def getMap(loc: MapLoc) =
    RpgMap.readFromDisk(project, loc.map)

  def layout(layoutTypeId: Int, sizeTypeId: Int, wArg: Float, hArg: Float) =
    Layout(layoutTypeId, sizeTypeId, wArg, hArg)

  def layoutWithOffset(layoutTypeId: Int, sizeTypeId: Int,
                       wArg: Float, hArg: Float,
                       xOffset: Float, yOffset: Float) =
    Layout(layoutTypeId, sizeTypeId, wArg, hArg, xOffset, yOffset)

  /*
   * Things to do with the player's location and camera
   */

  def setPlayerLoc(mapName: String, x: Float, y: Float) = syncRun {
    game.setPlayerLoc(MapLoc(mapName, x, y))
  }

  def teleport(mapName: String, x: Float, y: Float,
      transitionId: Int = Transitions.FADE.id) = syncRun {
    val loc = MapLoc(mapName, x, y)
    val map = getMap(loc)
    val settedTransition = getInt("useTransition")
    var transition = Transitions.get(transitionId)
    val fadeDuration = Transitions.fadeLength

    if(settedTransition != -1) transition = Transitions.get(settedTransition)

    game.mapScreen.scriptFactory.runFromFile(
      ResourceConstants.transitionsScript,
      "transition"+transition+"('"+mapName+"',"+x.toString()+","+y.toString()+","+fadeDuration.toString()+")")

    stopSound()

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
    endAlpha: Float,
    duration: Float) = syncRun {
    activeScreen.windowManager.setTransition(endAlpha, duration)
  }

  /**
   * @param   r               Between 0.0f and 1.0f.
   * @param   g               Between 0.0f and 1.0f.
   * @param   b               Between 0.0f and 1.0f.
   * @param   a               Between 0.0f and 1.0f.
   * @param   fadeDuration    In seconds. 0f means instantaneous
   */
  def tintScreen(r: Float, g: Float, b: Float, a: Float,
      fadeDuration: Float) = syncRun {
    def activeScreenTint = activeScreen.windowManager.tintColor
    // If no existing tint, set color immediately and tween alpha only.
    if (activeScreenTint.a == 0) {
      activeScreenTint.set(r, g, b, 0f)
    }

    activeScreen.windowManager.tintTweener.tweenTo(new Color(r, g, b, a),
        fadeDuration)
  }

  def startBattle(encounterId: Int, overrideBattleBackground: String,
      overrideBattleMusic: String, overrideBattleMusicVolume: Float) = {
    val mapMetadata = mapScreen.mapAndAssetsOption.get.map.metadata
    val battleBackground =
      if (overrideBattleBackground.isEmpty)
        mapMetadata.battleBackground
      else
        overrideBattleBackground

    val (battleMusic, battleMusicVolume) =
      if (overrideBattleMusic.isEmpty)
        (mapMetadata.battleMusic.get.sound, mapMetadata.battleMusic.get.volume)
      else
        (overrideBattleMusic, overrideBattleMusicVolume)

    syncRun {
      game.startBattle(encounterId, battleBackground, battleMusic,
          battleMusicVolume)
    }

    // Blocks until the battle screen finishes on way or the other
    game.battleScreen.finishChannel.read
  }

  def getEventX(id: Int):Int = {
    getEventEntityInfo(id).map { info =>
      return info.x.toInt
    }
    return 0
  }

  def getEventY(id: Int):Int = {
    getEventEntityInfo(id).map { info =>
      return info.y.toInt
    }
    return 0
  }

  def getEventDirection(id: Int):Int = {
    getEventEntityInfo(id).map { info =>
      return info.dir
    }
    return 0
  }

  def getPlayerX():Int = {
    return getPlayerEntityInfo.x.toInt
  }

  def getPlayerY():Int = {
    return getPlayerEntityInfo.y.toInt
  }

  def getPlayerDirection():Int = {
    return getPlayerEntityInfo.dir
  }

  def endBattleBackToMap() = {
    setTransition(1, 0.5f)
    sleep(0.5f)

    syncRun {
      game.setScreen(game.mapScreen)

      // TODO fix hack of manipulating mapScreen directly
      game.mapScreen.windowManager.setTransition(0, 0.5f)

      game.battleScreen.endBattle()
    }
  }

  def showPicture(slot: Int, name: String, layout: Layout) = syncRun {
    activeScreen.windowManager.showPictureByName(slot, name, layout, 1.0f)
  }

  def showPicture(slot: Int, name: String, layout: Layout, alpha:Float) = syncRun {
    activeScreen.windowManager.showPictureByName(slot, name, layout, alpha)
  }

  def hidePicture(slot: Int) = syncRun {
    activeScreen.windowManager.hidePicture(slot)
  }

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
                loop: Boolean, fadeDuration: Float) = syncRun {
    activeScreen.playMusic(slot, specOpt, loop, fadeDuration)
  }

  def playMusic(slot: Int, music: String, volume: Float, loop: Boolean,
                fadeDuration: Float) = syncRun {
    activeScreen.playMusic(
        slot, Some(SoundSpec(music, volume)), loop, fadeDuration)
  }

  def stopMusic(slot: Int, fadeDuration: Float) = syncRun {
    activeScreen.playMusic(
        slot, None, false, fadeDuration)
  }

  def playAnimation(animationId: Int, screenX: Float, screenY: Float,
      speedScale: Float) = syncRun {
    activeScreen.playAnimation(animationId,
        new FixedAnimationTarget(screenX, screenY), speedScale)
  }

  def playAnimationOnEvent(animationId: Int, eventId: Int, speedScale: Int) = {
    mapScreen.eventEntities.get(eventId) map { entity =>
      activeScreen.playAnimation(animationId,
          new MapEntityAnimationTarget(mapScreen, entity),
          speedScale)
    }
  }

  def playAnimationOnPlayer(animationId: Int, speedScale: Int) = {
    activeScreen.playAnimation(animationId,
        new MapEntityAnimationTarget(mapScreen, mapScreen.playerEntity),
        speedScale)
    val info = getPlayerEntityInfo()
    playAnimation(animationId, info.screenX, info.screenY, speedScale)
  }

  def playSound(sound: String) = syncRun {
    activeScreen.playSound(SoundSpec(sound))
  }

  def playSound(sound: String, volume: Float, pitch: Float) = syncRun {
    activeScreen.playSound(SoundSpec(sound, volume, pitch))
  }

  def stopSound() = syncRun {
    activeScreen.stopSound()
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
    layout: Layout,
    options: NativeObject): ChoiceWindow#ChoiceWindowScriptInterface = {
    newChoiceWindow(
        lines, layout,
        JsonUtils.nativeObjectToCaseClass[TextChoiceWindowOptions](options))
  }

  def newChoiceWindow(
    lines: Array[String],
    layout: Layout,
    options: TextChoiceWindowOptions): ChoiceWindow#ChoiceWindowScriptInterface = {
    val window = syncRun {
      new TextChoiceWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        lines,
        layout,
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

  def newTextWindow(text: Array[String], layout: Layout,
      options: NativeObject):
      PrintingTextWindow#PrintingTextWindowScriptInterface = {
    newTextWindow(text, layout,
        JsonUtils.nativeObjectToCaseClass[PrintingTextWindowOptions](options))
  }

  def newTextWindow(text: Array[String], layout: Layout,
      options: PrintingTextWindowOptions):
      PrintingTextWindow#PrintingTextWindowScriptInterface = {
    val window = syncRun {
      new PrintingTextWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        text,
        layout,
        options)
    }
    window.scriptInterface
  }

  def showText(text: Array[String]): Int = {
    val window = newTextWindow(
      text,
      layout(SOUTH, FIXED, 640, 180),
      PrintingTextWindowOptions(showArrow = true))
    window.awaitClose()
  }

  def getChoice(question: Array[String], choices: Array[String],
      allowCancel: Boolean) = {
    val questionLayout =
      layout(SOUTH, FIXED, 640, 180)
    val questionWindow = syncRun {
      new PrintingTextWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        question,
        questionLayout)
    }

    val fontbmp = activeScreen.windowManager.fontbmp
    val choicesWidth = Window.maxWidth(choices, fontbmp, TextChoiceWindow.xpad)
    // Removing 0.5*xpad at the end makes it look better.
    val choicesHeight =
      choices.length * WindowText.DefaultLineHeight +
      1.5f * TextChoiceWindow.ypad

    val choiceLayout = layoutWithOffset(
        SOUTHEAST, FIXED, choicesWidth, choicesHeight, 0, -questionLayout.h)

    val choiceWindow = newChoiceWindow(
        choices,
        choiceLayout,
        TextChoiceWindowOptions(
            allowCancel = allowCancel, justification = RIGHT))

    val choice = choiceWindow.getChoice()
    choiceWindow.close()

    questionWindow.scriptInterface.close()

    choice
  }

  def setWindowskin(windowskinPath: String) = syncRun {
    game.setWindowskin(windowskinPath)
  }

  def getPlayerEntityInfo(): EntityInfo = syncRun {
    mapScreen.getPlayerEntityInfo()
  }

  def getEventEntityInfo(id: Int): Option[EntityInfo] = {
    mapScreen.eventEntities.get(id).map(EntityInfo.apply(_, mapScreen))
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

  def modifyParty(add: Boolean, characterId: Int): Boolean = syncRun {
    if (characterId >= project.data.enums.characters.size)
      return false

    persistent.modifyParty(add, characterId)
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

  def countItems(itemId: Int) = syncRun {
    persistent.countItems(itemId)
  }

  def addRemoveGold(delta: Int) = syncRun {
    persistent.addRemoveGold(delta)
  }

  def useItemInMenu(itemId: Int, characterId: Int) = syncRun {
    if (persistent.addRemoveItem(itemId, -1)) {
      val item = project.data.enums.items(itemId)
      val characterStatus = BattleStatus.fromCharacter(
          project.data,
          persistent.getPartyParameters(project.data.enums.characters),
          characterId, index = -1)

      val damages = item.effects.flatMap(_.applyAsSkillOrItem(characterStatus))

      for (damage <- damages) {
        logger.debug("Character %d took %d damage from item.".format(
            characterId, damage.value))
      }

      characterStatus.clampVitals()

      persistent.saveCharacterVitals(characterId, characterStatus.hp,
          characterStatus.mp, characterStatus.tempStatusEffectIds)
    }
  }

  /**
   * @param   hpPercentage    Between 0.0f and 1.0f.
   * @param   mpPercentage    Between 0.0f and 1.0f.
   */
  def healCharacter(characterId: Int, hpPercentage: Float,
      mpPercentage: Float, removeStatusEffects: Boolean = false) = syncRun {
    val characterStatus = BattleStatus.fromCharacter(
        project.data,
        persistent.getPartyParameters(project.data.enums.characters),
        characterId, index = -1)

    if (removeStatusEffects) {
      characterStatus.updateTempStatusEffectIds(Array.empty)
    }

    characterStatus.hp +=
      (characterStatus.stats.mhp * hpPercentage).round
    characterStatus.mp +=
      (characterStatus.stats.mhp * mpPercentage).round

    characterStatus.clampVitals()

    persistent.saveCharacterVitals(characterId, characterStatus.hp,
        characterStatus.mp, characterStatus.tempStatusEffectIds)
  }

  def healParty(hpPercentage: Float, mpPercentage: Float,
      removeStatusEffects: Boolean = false) = syncRun {
    for (characterId <- persistent.getIntArray(PARTY)) {
      healCharacter(characterId, hpPercentage, mpPercentage,
          removeStatusEffects)
    }
  }

  def damageCharacter(characterId: Int, hpPercentage: Float,
      mpPercentage: Float) =
    healCharacter(characterId, -hpPercentage, -mpPercentage)

  def damageParty(hpPercentage: Float, mpPercentage: Float) =
    healParty(-hpPercentage, -mpPercentage)

  def getBattleStats(characterId: Int, proposedSlotId: Int,
      proposedItemId: Int) = {
    val partyParams = syncRun {
      persistent.getPartyParameters(project.data.enums.characters)
    }
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

  def drawText(id:Int,text:String , x:Int, y:Int, color:Color=new Color(255,255,255,1), scale:Float=1.0f) = syncRun {
      logger.debug("drawText: "+text+" on ");
      mapScreen.windowManager.addDrawText(new ScreenText(id, text, x, y, color, scale))
  }

  def removeDrawedText(id:Int) = syncRun {
    mapScreen.windowManager.removeDrawText(id)
  }

  def color(r:Float, g:Float, b:Float, alpha:Float):Color = {
    var R = r/255
    var G = g/255
    var B = b/255

    return new Color(R,G,B,alpha)
  }

  def log(text: String) = syncRun {
    logger.debug(text)
  }

  def takeDamage(characterId: Int, hp:Int, mp:Int) = syncRun {
    val characterStatus = BattleStatus.fromCharacter(
        project.data,
        persistent.getPartyParameters(project.data.enums.characters),
        characterId, index = -1)

    characterStatus.hp -= hp
    characterStatus.mp -= mp

    characterStatus.clampVitals()

    persistent.saveCharacterVitals(characterId, characterStatus.hp,
        characterStatus.mp, characterStatus.tempStatusEffectIds)
  }

  // TODO: built it in
  def keyPress(key: String) = {

  }

  def getScriptAsString(scriptPath: String): String = {
    Script.readFromDisk(project, scriptPath).readAsString
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