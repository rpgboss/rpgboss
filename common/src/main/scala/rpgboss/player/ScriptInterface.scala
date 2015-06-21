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
import scalaj.http.Http
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

case class EntityInfo(x: Float = 0, y: Float = 0, dir: Int = 0,
                      screenX: Float = 0, screenY: Float = 0, screenTopLeftX: Float = 0, screenTopLeftY: Float = 0, width: Float = 0, height: Float = 0)

object EntityInfo {
  def apply(e: Entity, mapScreen: MapScreen): EntityInfo = {
    val pxPerTileX = mapScreen.screenW / mapScreen.screenWTiles
    val pxPerTileY = mapScreen.screenH / mapScreen.screenHTiles
    val screenX =
      (e.x - mapScreen.camera.x) * pxPerTileX + (mapScreen.screenW / 2)
    val screenY =
      (e.y - mapScreen.camera.y) * pxPerTileY + (mapScreen.screenH / 2)
    val width = pxPerTileX * e.graphicW
    val height = pxPerTileY * e.graphicH
    val screenTopLeftX = screenX - width / 2
    val screenTopLeftY = screenY - height / 2

    apply(e.x, e.y, e.dir, screenX, screenY, screenTopLeftX, screenTopLeftY, width, height)
  }
}

case class CurrentAndProposedStats(
  current: BattleStats, proposed: BattleStats)

trait HasScriptConstants {
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right

  val PLAYER_LOC = "playerLoc"
  def VEHICLE_LOC(vehicleId: Int) = "vehicleLoc-%d".format(vehicleId)

  val GOLD = "gold"
  val PLAYER_MOVEMENT_LOCKS = "playerMovementLocks"

  val EVENTS_ENABLED = "eventsEnabled"
  val MENU_ENABLED = "menuEnabled"

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

  def CHARACTER_LEARNED_SKILLS(characterId: Int) =
    "characterLearnedSkills-%d".format(characterId)

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
    def f() = {
      try {
        op
      } catch {
        case e: scala.runtime.NonLocalReturnControl[T] => e.value
      }
    }

    if (onBoundThread())
      f()
    else
      GdxUtils.syncRun(f)
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

  /**
   * FIXME: This method has a lot of problems.
   *
   *  1. This method cannot be posted to the main Gdx thread, as it has some
   *     game.sleep(...) calls, which would hang the whole game.
   *
   *  2. Unfortunately, game.setPlayerLoc will kill any event script threads,
   *     which is generally the thread that this is running on.
   *
   * Thus the only way to use this safely is to call the setPlayerLoc method
   * LAST in this whole function. That means that in transitions.js,
   * setPlayerLoc must be the last call.
   *
   * Unfortunately this still causes a whole barf of exceptions to appear every
   * time the player switches maps.
   *
   * This is very fragile and should be simplified / removed. Probably by
   * removing the custom transitions scripts.
   */
  def teleport(mapName: String, x: Float, y: Float,
               transitionId: Int = Transitions.FADE.id) = {
    val loc = MapLoc(mapName, x, y)
    val settedTransition = getInt("useTransition")
    var transition = Transitions.get(transitionId)
    val fadeDuration = Transitions.fadeLength

    if (settedTransition != -1) transition = Transitions.get(settedTransition)

    stopSound()

    // This must be last, as the actual setPlayerLoc method is called in these
    // methods, and the thread is killed once that method is called.
    game.mapScreen.scriptFactory.runFromFile(
      ResourceConstants.transitionsScript,
      "transition" + transition + "('" + mapName + "'," + x.toString() + "," + y.toString() + "," + fadeDuration.toString() + ")",
      runOnNewThread = false)
  }

  def placeVehicle(vehicleId: Int, mapName: String, x: Float,
                   y: Float) = syncRun {
    val loc = MapLoc(mapName, x, y)

    if (!loc.isEmpty) {
      mapScreen.mapAndAssetsOption.map { mapAndAssets =>
        if (mapAndAssets.mapName == mapName) {
          mapScreen.insertVehicleEntity(vehicleId, loc)
        }
      }
    }

    persistent.setLoc(VEHICLE_LOC(vehicleId), loc)
  }

  def setPlayerInVehicle(inVehicle: Boolean, vehicleId: Int) = syncRun {
    mapScreen.playerEntity.setInVehicle(inVehicle, vehicleId)
  }

  /**
   * Moves the map camera.
   */
  def moveCamera(dx: Float, dy: Float, async: Boolean, duration: Float) = {
    val move = syncRun { mapScreen.camera.enqueueMove(dx, dy, duration) }
    if (!async)
      move.awaitFinish()
  }

  /**
   * Gets the position of the map camera.
   */
  def getCameraPos() = syncRun {
    mapScreen.camera.info
  }

  def setCameraFollowEvent(eventId: Int) = syncRun {
    mapScreen.setCameraFollow(Some(eventId))
  }

  def setCameraFollowPlayer() = syncRun {
    mapScreen.setCameraFollow(Some(EntitySpec.playerEntityId))
  }

  def setCameraFollowNone() = syncRun {
    mapScreen.setCameraFollow(None)
  }

  /*
   * Things to do with the screen
   */

  def setTransition(
    endAlpha: Float,
    duration: Float) = syncRun {
    activeScreen.windowManager.setTransition(endAlpha, duration)
  }

  def shakeScreen(xAmplitude: Float, yAmplitude: Float, frequency: Float,
      duration: Float) = syncRun {
    activeScreen.shakeManager.startShake(
        xAmplitude, yAmplitude, frequency, duration)
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

  def overrideMapBattleSettings(battleBackground: String,
                                battleMusic: String,
                                battleMusicVolume: Float,
                                randomEncountersOn: Boolean) = syncRun {
    mapScreen.mapAndAssetsOption map {
      _.setOverrideBattleSettings(battleBackground, battleMusic,
          battleMusicVolume, randomEncountersOn)
    }
  }

  def startBattle(encounterId: Int) = {
    syncRun {
      game.startBattle(encounterId)
    }

    // Blocks until the battle screen finishes on way or the other
    game.battleScreen.finishChannel.read
  }

  def setTimer(time: Int) = {
    setInt("timer", time)
  }

  def clearTimer() = {
    // set it way below 0 to does not make problems with conditions
    setInt("timer", -5000)
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

  def showPicture(slot: Int, name: String, layout: Layout, alpha: Float) = syncRun {
    activeScreen.windowManager.showPictureByName(slot, name, layout, alpha)
  }

  def showPictureLoop(slot: Int, folderPath: String, layout: Layout,
                      alpha: Float, fps: Int) = syncRun {
    activeScreen.windowManager.showPictureLoop(slot, folderPath, layout, alpha, fps)
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
                    speedScale: Float, sizeScale: Float) = syncRun {
    activeScreen.playAnimation(animationId,
      new FixedAnimationTarget(screenX, screenY), speedScale, sizeScale)
  }

  def playAnimationOnEvent(animationId: Int, eventId: Int, speedScale: Float,
                           sizeScale: Float) = {
    mapScreen.allEntities.get(eventId) map { entity =>
      activeScreen.playAnimation(animationId,
        new MapEntityAnimationTarget(mapScreen, entity),
        speedScale, sizeScale)
    }
  }

  def playAnimationOnPlayer(animationId: Int, speedScale: Float,
                            sizeScale: Float) = {
    activeScreen.playAnimation(animationId,
      new MapEntityAnimationTarget(mapScreen, mapScreen.playerEntity),
      speedScale, sizeScale)
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

  def httpRequest(url: String): String = {
    val result = Http(url).asString
    return result.toString()
  }

  /*
   * Things to do with user interaction
   */
  def sleep(duration: Float) = {
    assert(!onBoundThread(),
      "Do not use game.sleep on the main GDX thread. That hangs the game.")
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
                    options: NativeObject): PrintingTextWindow#PrintingTextWindowScriptInterface = {
    newTextWindow(text, layout,
      JsonUtils.nativeObjectToCaseClass[PrintingTextWindowOptions](options))
  }

  def newTextWindow(
    text: Array[String],
    layout: Layout = Layout(SOUTH, FIXED, 640, 180),
    options: PrintingTextWindowOptions = PrintingTextWindowOptions()): PrintingTextWindow#PrintingTextWindowScriptInterface = {
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

  def showTextScala(
    text: Array[String],
    options: PrintingTextWindowOptions = PrintingTextWindowOptions()): Int = {
    val window = newTextWindow(text, options = options)
    window.awaitClose()
  }

  def showTextScala(text: Array[String], options: NativeObject): Int = {
    showTextScala(
      text,
      JsonUtils.nativeObjectToCaseClass[PrintingTextWindowOptions](options))
  }

  def getChoice(
    question: Array[String],
    choices: Array[String],
    allowCancel: Boolean,
    questionOptions: PrintingTextWindowOptions = PrintingTextWindowOptions()) = {
    val questionWindow = newTextWindow(question, options = questionOptions)

    val fontbmp = activeScreen.windowManager.fontbmp
    val choicesWidth = Window.maxWidth(choices, fontbmp, TextChoiceWindow.xpad)
    // Removing 0.5*xpad at the end makes it look better.
    val choicesHeight =
      choices.length * WindowText.DefaultLineHeight +
        1.5f * TextChoiceWindow.ypad

    val choiceLayout = layoutWithOffset(
      SOUTHEAST, FIXED, choicesWidth, choicesHeight, 0,
      -questionWindow.getRect().h)

    val choiceWindow = newChoiceWindow(
      choices,
      choiceLayout,
      TextChoiceWindowOptions(
        allowCancel = allowCancel, justification = RIGHT))

    val choice = choiceWindow.getChoice()
    choiceWindow.close()

    questionWindow.close()

    choice
  }

  def getChoice(
    question: Array[String],
    choices: Array[String],
    allowCancel: Boolean,
    questionOptions: NativeObject): Int = {
    getChoice(question, choices, allowCancel,
      JsonUtils.nativeObjectToCaseClass[PrintingTextWindowOptions](
        questionOptions))
  }

  def setWindowskin(windowskinPath: String) = syncRun {
    game.setWindowskin(windowskinPath)
  }

  def getPlayerEntityInfo(): EntityInfo = syncRun {
    mapScreen.getPlayerEntityInfo()
  }

  def getEventEntityInfo(id: Int): EntityInfo = {
    mapScreen.allEntities.get(id).map(EntityInfo.apply(_, mapScreen)).orNull
  }

  /**
   * Returns -1 on error. Life percentage rounded to integer (0-100) otherwise.
   */
  def getEnemyLifePercentage(enemyId: Int): Int = syncRun {
    if (activeScreen != game.battleScreen || game.battleScreen.battle.isEmpty)
      return -1

    val battle = game.battleScreen.battle.get
    if (enemyId < 0 || enemyId >= battle.enemyStatus.length)
      return -1

    val enemy = battle.enemyStatus(enemyId)
    val result = ((enemy.hp.toFloat / enemy.stats.mhp) * 100).round.toInt
    assert(result >= 0)
    assert(result <= 100)
    return result
  }

  /**
   * Returns true if successful, false otherwise.
   */
  def setEnemyVitals(enemyId: Int, hpPercentage: Float,
                     mpPercentage: Float): Boolean = syncRun {
    if (activeScreen != game.battleScreen || game.battleScreen.battle.isEmpty)
      return false

    val battle = game.battleScreen.battle.get
    if (enemyId < 0 || enemyId >= battle.enemyStatus.length)
      return false

    val enemy = battle.enemyStatus(enemyId)
    enemy.hp = (enemy.stats.mhp * hpPercentage).round
    enemy.mp = (enemy.stats.mmp * mpPercentage).round
    return true
  }

  def activateEvent(id: Int, awaitFinish: Boolean) = {
    val eventOpt = mapScreen.allEntities.get(id)

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
    val move = syncRun {
      mapScreen.moveEvent(id, dx, dy, affixDirection)
    }
    if (move != null && !async)
      move.awaitFinish()
  }

  def movePlayer(dx: Float, dy: Float,
                 affixDirection: Boolean = false,
                 async: Boolean = false) = {
    val move = syncRun {
      mapScreen.movePlayer(dx, dy, affixDirection)
    }
    if (move != null && !async)
      move.awaitFinish()
  }

  /**
   * Returns true if succeeds.
   */
  def exitVehicle(): Boolean = {
    def playerEntity = mapScreen.playerEntity
    val (ux, uy) = playerEntity.getDirectionUnitVector()
    for (i <- 0 to 10) {
      val dx = ux * i * 0.1f
      val dy = uy * i * 0.1f
      if (playerEntity.canStandAt(dx, dy)) {
        syncRun {
          setLoc(
              VEHICLE_LOC(playerEntity.inVehicleId),
              MapLoc(playerEntity.mapName.get, playerEntity.x, playerEntity.y))
          playerEntity.setInVehicle(false, -1)
        }
        setPlayerCollision(false)
        movePlayer(dx, dy)
        setPlayerCollision(true)
        return true
      }
    }

    return false
  }

  def setPlayerCollision(collisionOn: Boolean) = syncRun {
    mapScreen.playerEntity._collisionOn = collisionOn
  }

  def setEventSpeed(id: Int, speed: Float) = syncRun {
    mapScreen.allEntities.get(id).map(_.speed = speed)
  }

  def setPlayerSpeed(speed: Float) = syncRun {
    mapScreen.allEntities.get(EntitySpec.playerEntityId).map(_.speed = speed)
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

  def getParty() = syncRun {
    persistent.getIntArray(PARTY)
  }

  def modifyParty(add: Boolean, characterId: Int): Boolean = syncRun {
    if (characterId >= project.data.enums.characters.size)
      return false

    persistent.modifyParty(add, characterId)
  }

  def giveExperience(
    characterIds: Array[Int],
    experience: Int,
    showNotifications: Boolean) = {
    val leveled = syncRun {
      game.persistent.giveExperience(
        project.data,
        characterIds,
        experience)
    }

    if (showNotifications) {
      val leveledCharacterNames = leveled.map(getCharacterName(_))
      showTextScala(Array("Received %d XP.".format(experience)))
      for (name <- leveledCharacterNames) {
        showTextScala(Array("%s leveled!".format(name)))
      }
    }
  }

  def giveCharacterExperience(characterId: Int, experience: Int,
                              showNotifications: Boolean) = {
    giveExperience(Array(characterId), experience, showNotifications)
  }

  def givePartyExperience(experience: Int, showNotifications: Boolean) = {
    giveExperience(getParty(), experience, showNotifications)
  }

  def setLevels(characterIds: Array[Int], level: Int) = syncRun {
    game.persistent.setCharacterLevels(project.data, characterIds, level)
  }

  def setCharacterLevel(characterId: Int, level: Int) = {
    setLevels(Array(characterId), level)
  }

  def setPartyLevel(level: Int) = {
    setLevels(getParty(), level)
  }

  def openStore(itemIdsSold: Array[Int], buyPriceMultiplier: Float,
                sellPriceMultiplier: Float) = {
    assert(activeScreen == game.mapScreen)
    val finishable = syncRun {
      val statement = EventJavascript.jsStatement(
        "openStore", itemIdsSold, buyPriceMultiplier, sellPriceMultiplier)
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

  def addRemoveSkill(add: Boolean, characterId: Int, skillId: Int) = syncRun {
    persistent.addRemoveLearnedSkills(add, characterId, skillId)
  }

  def getKnownSkills(characterId: Int): Array[Int] = syncRun {
    val characterStatus = BattleStatus.fromCharacter(
      project.data,
      persistent.getPartyParameters(project.data.enums.characters),
      characterId, index = -1)

    val allSkills = project.data.enums.skills
    characterStatus.knownSkillIds
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

  def useSkillInMenu(casterCharacterId: Int, skillId: Int,
      targetCharacterId: Int) = syncRun {
    assert(skillId < project.data.enums.skills.length)

    val skill = project.data.enums.skills(skillId)

    val casterStatus = BattleStatus.fromCharacter(
        project.data,
        persistent.getPartyParameters(project.data.enums.characters),
        casterCharacterId, index = -1)

    // Reuse the existing object if it's a self-cast. Otherwise, saving the
    // character vitals below has unexpected results.
    val targetStatus =
      if (targetCharacterId != casterCharacterId) {
        BattleStatus.fromCharacter(
            project.data,
            persistent.getPartyParameters(project.data.enums.characters),
            targetCharacterId, index = -1)
      } else {
        casterStatus
      }

    assert(skill.cost <= casterStatus.mp)

    casterStatus.mp -= skill.cost

    skill.applySkill(casterStatus, targetStatus)

    persistent.saveCharacterVitals(casterCharacterId, casterStatus.hp,
        casterStatus.mp, casterStatus.tempStatusEffectIds)

    if (targetCharacterId != casterCharacterId) {
      persistent.saveCharacterVitals(targetCharacterId, targetStatus.hp,
          targetStatus.mp, targetStatus.tempStatusEffectIds)
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
      (characterStatus.stats.mmp * mpPercentage).round

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

  def addInt(key: String, value: Int) = syncRun {
    var currentValue = getInt(key)
    currentValue += value
    setInt(key, currentValue)
  }

  def substractInt(key: String, value: Int) = syncRun {
    var currentValue = getInt(key)
    currentValue -= value
    setInt(key, currentValue)
  }

  def multiplyInt(key: String, value: Int) = syncRun {
    var currentValue = getInt(key)
    currentValue *= value
    setInt(key, currentValue)
  }

  def divideInt(key: String, value: Int) = syncRun {
    var currentValue = getInt(key)
    currentValue /= value
    setInt(key, currentValue)
  }

  def modInt(key: String, value: Int) = syncRun {
    var currentValue = getInt(key)
    currentValue = currentValue % value
    setInt(key, currentValue)
  }

  def getString(key: String) = syncRun {
    persistent.getString(key)
  }
  def setString(key: String, value: String) = syncRun {
    persistent.setString(key, value)
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

  def setStringArrayElement(key: String, index: Int, value: String) = syncRun {
    val array = persistent.getStringArray(key)
    array.update(index, value)
    persistent.setStringArray(key, array)
  }

  def getLoc(key: String) = syncRun {
    persistent.getLoc(key)
  }

  def setLoc(key: String, loc: MapLoc) = syncRun {
    persistent.setLoc(key, loc)
  }

  def getCharacterName(characterId: Int) = syncRun {
    persistent.getCharacterName(project.data, characterId)
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

  def toTitleScreen() = syncRun {
    game.gameOver()
  }

  def runScript(scriptPath: String, functionToCall: String) = {
    game.mapScreen.scriptFactory.runFromFile(
      scriptPath, functionToCall, runOnNewThread = false)
  }

  def drawText(id: Int, text: String, x: Int, y: Int, color: Color = new Color(255, 255, 255, 1), size: Int = 12) = syncRun {
    logger.debug("drawText: " + id + ", text: " + text + " on " + x + ", " + y + ", size:"+size);
    mapScreen.windowManager.addDrawText(new ScreenText(id, text, x, y, color, size))
  }

  def drawRectangle(id: Int, x: Int, y: Int, width: Int, height: Int, color: Color = new Color(255, 255, 255, 1), recttype: String = "filled", radius: Int = 0) = syncRun {
    logger.debug("drawRectangle: " + id + ", size: " + width + "x" + height + " on " + x + ", " + y);

    var typeof = ShapeType.Filled
    if (recttype == "filled") {
      typeof = ShapeType.Filled
    }
    if (recttype == "line") {
      typeof = ShapeType.Line
    }
    if (radius == 0) {
      mapScreen.windowManager.addDrawRectangle(new Rectangle(id, x, y, width, height, color, typeof))
    } else {
      mapScreen.windowManager.addDrawRectangle(new RoundedRectangle(id, radius, x, y, width, height, color, typeof))
    }
  }

  def removeDrawedText(id: Int) = syncRun {
    mapScreen.windowManager.removeDrawText(id)
  }

  def removeDrawedRectangle(id: Int) = syncRun {
    mapScreen.windowManager.removeDrawRectangle(id)
  }

  def color(r: Float, g: Float, b: Float, alpha: Float): Color = {
    var R = r / 255
    var G = g / 255
    var B = b / 255

    return new Color(R, G, B, alpha)
  }

  def log(text: String) = syncRun {
    logger.debug(text)
  }

  def takeDamage(characterId: Int, hp: Int, mp: Int) = syncRun {
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

  def getKeyInput(capturedKeys: Array[Int]): Int = {
    val inputHandler = syncRun {
      val inputHandler = new OneTimeInputHandler(capturedKeys.toSet)
      activeScreen.inputs.prepend(inputHandler)
      inputHandler
    }

    val result = inputHandler.awaitFinish()
    syncRun {
      activeScreen.inputs.remove(inputHandler)
    }

    return result
  }

  /**
   * Returns the value associated with the message key, or "$key$" otherwise.
   */
  def getMessage(key: String) = {
    project.data.messages.get(key).getOrElse("$%s$".format(key))
  }

  def getMapName(): String = {
    return mapScreen.mapName.get
  }

  def getScriptAsString(scriptPath: String): String = {
    Script.readFromDisk(project, scriptPath).readAsString
  }

  def addScriptHook(jsFunction: org.mozilla.javascript.Function) = syncRun {
    mapScreen.scriptHooks.addScriptHook(jsFunction)
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