package rpgboss.player

import aurelienribon.tweenengine._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.resource._
import rpgboss.player.entity._
import Predef._

case class EntityInfo(x: Float, y: Float, dir: Int)

object EntityInfo {
  def apply(e: Entity): EntityInfo = apply(e.x, e.y, e.dir)
}

trait HasScriptConstants {
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right

  val PLAYER_X = "playerX"
  val PLAYER_Y = "playerY"
  val PLAYER_MAP_NAME = "playerMapName"

  val PLAYER_MOVEMENT_LOCKS = "playerMovementLocks"

  val PARTY = "party"
  val INVENTORY_IDXS = "inventoryIdxs"
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

  val PICTURE_SLOTS = Constants.PictureSlots
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
  with ThreadChecked {
  assume(activeScreen != null)

  private def mapScreen = game.mapScreen
  private def persistent = game.persistent
  private def project = game.project

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

  def setPlayerLoc(loc: MapLoc) = syncRun {
    persistent.setInt(PLAYER_X, loc.x.round)
    persistent.setInt(PLAYER_Y, loc.y.round)

    if (mapScreen != null) {
      mapScreen.playerEntity.x = loc.x
      mapScreen.playerEntity.y = loc.y
      mapScreen.playerEntity.mapName = Some(loc.map)

      mapScreen.updateMapAssets(if(loc.map.isEmpty) None else Some(loc.map))
    }
  }

  /**
   * Moves the map camera.
   */
  def moveCamera(dx: Float, dy: Float, async: Boolean) = {
    val move = syncRun { mapScreen.camera.enqueueMove(dx, dy) }
    if (!async)
      move.awaitDone()
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
    assert(encounterId >= 0)
    assert(encounterId < project.data.enums.encounters.length)

    assert(game.getScreen() == game.mapScreen)

    // Fade out map
    setTransition(0, 1, 0.6f)
    sleep(0.6f)

    syncRun {
      game.setScreen(game.battleScreen)

      // TODO fix this hack of manipulating battleScreen directly
      game.battleScreen.windowManager.setTransition(1, 0, 0.6f)

      val encounter = project.data.enums.encounters(encounterId)
      val charactersIdxs =
        (0 until project.data.enums.characters.length).toArray

      val battle = new Battle(
        project.data,
        persistent.getIntArray(PARTY),
        persistent.getIntArray(CHARACTER_LEVELS),
        persistent.getIntArray(CHARACTER_HPS),
        persistent.getIntArray(CHARACTER_MPS),
        charactersIdxs.map(id => persistent.getIntArray(CHARACTER_EQUIP(id))),
        charactersIdxs.map(
          id => persistent.getIntArray(CHARACTER_STATUS_EFFECTS(id))),
        persistent.getIntArray(CHARACTER_ROWS),
        encounter,
        aiOpt = Some(new RandomEnemyAI))

      game.battleScreen.startBattle(battle, battleBackground)
    }
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

  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int,
                  h: Int) = syncRun {
    activeScreen.windowManager.showPictureByName(slot, name, x, y, w, h)
  }

  def hidePicture(slot: Int) = syncRun {
    activeScreen.windowManager.hidePicture(slot)
  }

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
    loop: Boolean, fadeDuration: Float) = syncRun {

    mapScreen.musics(slot).map({ oldMusic =>
      val tweenMusic = new GdxMusicTweenable(oldMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDuration)
        .target(0f)
        .setCallback(new TweenCallback {
          override def onEvent(typeArg: Int, x: BaseTween[_]) = {
            if (typeArg == TweenCallback.COMPLETE) {
              oldMusic.stop()
            }
          }
        }).start(mapScreen.tweenManager)
    })

    mapScreen.musics(slot) = specOpt.map { spec =>
      val resource = Music.readFromDisk(project, spec.sound)
      resource.loadAsset(game.assets)
      // TODO: fix this blocking call
      game.assets.finishLoading()
      val newMusic = resource.getAsset(game.assets)

      // Start at zero volume and fade to desired volume
      newMusic.stop()
      newMusic.setVolume(0f)
      newMusic.setLooping(loop)
      newMusic.play()

      // Setup volume tween
      val tweenMusic = new GdxMusicTweenable(newMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDuration)
        .target(spec.volume).start(mapScreen.tweenManager)

      newMusic
    }
  }

  /*
   * Things to do with user interaction
   */
  def sleep(duration: Float) = {
    Thread.sleep((duration * 1000).toInt)
  }

  def newChoiceWindow(
    lines: Array[String],
    x: Int, y: Int, w: Int, h: Int,
    justification: Int,
    columns: Int,
    displayedLines: Int,
    allowCancel: Boolean) = {
    val window = syncRun {
      new TextChoiceWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        lines,
        x, y, w, h,
        justification = justification,
        columns = columns,
        displayedLines = displayedLines,
        allowCancel = allowCancel)
    }

    window.scriptInterface
  }

  def newChoiceWindow(
    choices: Array[String],
    x: Int, y: Int, w: Int, h: Int): ChoiceWindow#ChoiceWindowScriptInterface =
    newChoiceWindow(choices, x, y, w, h,
      Window.Left,
      1 /* columns */ ,
      0 /* displayedChoices */ ,
      false /* allowCancel */ )

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

    val window = syncRun {
      new SpatialChoiceWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        choices.map(x => IntRect(x(0), x(1), x(2), x(3))),
        defaultChoice = defaultChoice)
    }

    val choice = window.scriptInterface.getChoice()
    window.scriptInterface.close()
    choice
  }

  def showText(text: Array[String], x: Int, y: Int, w: Int, h: Int,
               timePerChar: Float) = {
    val window = syncRun {
      new PrintingTextWindow(
        game.persistent,
        activeScreen.windowManager,
        activeScreen.inputs,
        text,
        x, y, w, h,
        timePerChar)
    }
    window.scriptInterface.awaitClose()
  }

  def showText(text: Array[String]): Unit =
    showText(text, x = 0, y = 300, w = 640, h = 180, timePerChar = 0.02f)

  def getPlayerEntityInfo(): EntityInfo = syncRun {
    EntityInfo(mapScreen.playerEntity)
  }

  def getEventEntityInfo(id: Int): Option[EntityInfo] = {
    mapScreen.eventEntities.get(id).map(EntityInfo.apply)
  }

  def movePlayer(dx: Float, dy: Float,
                 affixDirection: Boolean = false,
                 async: Boolean = false) = {
    moveEntity(mapScreen.playerEntity, dx, dy, affixDirection, async)
  }

  def activateEvent(id: Int, awaitFinish: Boolean) = {
    val eventOpt = mapScreen.eventEntities.get(id)
    val scriptOpt = eventOpt.flatMap(_.activate(SpriteSpec.Directions.NONE))

    if (awaitFinish)
      scriptOpt.map(_.awaitFinish())

    scriptOpt.isDefined
  }

  def moveEvent(id: Int, dx: Float, dy: Float,
                affixDirection: Boolean = false,
                async: Boolean = false) = {
    val entityOpt = mapScreen.eventEntities.get(id)
    entityOpt.foreach { entity =>
      moveEntity(entity, dx, dy, affixDirection, async)
    }
    entityOpt.isDefined
  }

  private def moveEntity(entity: Entity, dx: Float, dy: Float,
                         affixDirection: Boolean = false,
                         async: Boolean = false) = {
    import SpriteSpec.Directions._
    if (dx != 0 || dy != 0) {
      if (!affixDirection) {
        val direction =
          if (math.abs(dx) > math.abs(dy))
            if (dx > 0) EAST else WEST
          else
            if (dy > 0) SOUTH else NORTH
        syncRun { entity.enqueueMove(EntityFaceDirection(direction)) }
      }

      val move = EntityMove(dx, dy)
      syncRun { entity.enqueueMove(move) }

      if (!async)
        move.awaitDone()
    }
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

  def startNewGame() = syncRun {
    game.startNewGame()
  }
  def quit() = syncRun {
    game.quit()
  }
}

object ScriptInterfaceConstants extends HasScriptConstants {

}