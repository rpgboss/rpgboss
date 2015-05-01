package rpgboss.player

import com.badlogic.gdx.Game
import java.io.File
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import rpgboss.lib._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import java.util.concurrent.Executors
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import rpgboss.model.resource.RpgAssetManager
import java.lang.Thread.UncaughtExceptionHandler
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import rpgboss.lib.GdxUtils
import com.badlogic.gdx.Screen
import rpgboss.save.SaveFile
import rpgboss.model.battle.Battle
import rpgboss.model.battle.RandomEnemyAI

case class MutableMapLoc(
  var map: String = "",
  var x: Float = 0,
  var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)

  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }

  def set(other: MutableMapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

class RpgGame(gamepath: File)
  extends Game
  with HasScriptConstants
  with ThreadChecked {
  val project = Project.readFromDisk(gamepath).get

  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger()

  var startScreen: StartScreen = null
  var mapScreen: MapScreen = null
  var battleScreen: BattleScreen = null

  var renderingOffForTesting = false

  // Generate and pack sprites
  val spritesets = Map() ++ Spriteset.list(project).map(
    name => (name, Spriteset.readFromDisk(project, name)))
  var atlasSprites: TextureAtlas = null

  /*
   * SpriteBatch manages its own matrices. By default, it sets its modelview
   * matrix to the identity, and the projection matrix to an orthographic
   * projection with its lower left corner of the screen at (0, 0) and its
   * upper right corner at (Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
   *
   * This makes the eye-coordinates the same as the screen-coordinates.
   *
   * If you'd like to specify your objects in some other space, simply
   * change the projection and modelview (transform) matrices.
   */

  var persistent: PersistentState = null

  val assets = new RpgAssetManager(project)

  override def getScreen() = super.getScreen().asInstanceOf[RpgScreen]

  def create() = {
    rebindToCurrentThread()

    com.badlogic.gdx.utils.Timer.instance().start()

    if (!renderingOffForTesting)
      atlasSprites = GdxUtils.generateSpritesTextureAtlas(spritesets.values)

    persistent = new PersistentState()

    // TODO: Make configurable screen pixel dimensions
    startScreen = new StartScreen(this)
    battleScreen =
      new BattleScreen(Some(this), assets, atlasSprites, project,
        project.data.startup.screenW, project.data.startup.screenH,
        renderingOffForTesting)
    mapScreen = new MapScreen(this)

    beginGame()
  }

  def beginGame(): Unit = {
    assertOnBoundThread()

    setScreen(startScreen)

    // Standard setting for transitions
    persistent.setInt("useTransition",-1)

    // Standard setting for menu enabled/disabled
    persistent.setInt("menuEnabled",1)

    startScreen.scriptFactory.runFromFile(
      ResourceConstants.systemStartScript,
      ResourceConstants.systemStartCall)
  }

  def loadUserMainScript = {
    val customScript = Script.readFromDisk(project, ResourceConstants.mainScript)
    if(customScript.newDataStream != null) {
      mapScreen.scriptFactory.runFromFile(
        ResourceConstants.mainScript,
        "main()")
    }

    val timerScript = Script.readFromDisk(project, ResourceConstants.timerScript)
    if(timerScript.newDataStream != null) {
      mapScreen.scriptFactory.runFromFile(
        ResourceConstants.timerScript)
    }
  }

  /**
   * Sets the members of the player's party. Controls the sprite for both
   * walking on the map, as well as the party members in a battle.
   *
   * TODO: Figure out if this requires @partyArray to be non-empty.
   */
  def setParty(partyArray: Array[Int]) = {
    assertOnBoundThread()
    persistent.setIntArray(PARTY, partyArray)
  }

  def startNewGame() = {
    assertOnBoundThread()

    persistent.setInt(EVENTS_ENABLED, 1)
    persistent.setInt(MENU_ENABLED, 1)

    setParty(project.data.startup.startingParty.toArray)
    // Initialize data structures

    var characters = project.data.enums.characters.toArray
    persistent.setStringArray(CHARACTER_NAMES, characters.map(_.name))

    persistent.setIntArray(CHARACTER_LEVELS, characters.map(_.initLevel))

    val characterStats = for (c <- characters)
      yield BattleStats(project.data, c.baseStats(project.data, c.initLevel),
      c.startingEquipment)

    persistent.setIntArray(CHARACTER_HPS, characterStats.map(_.mhp))
    persistent.setIntArray(CHARACTER_MPS, characterStats.map(_.mmp))

    persistent.setIntArray(CHARACTER_EXPS, characters.map(x => 0))

    persistent.setIntArray(CHARACTER_ROWS, characters.map(x => 0))

    setPlayerLoc(project.data.startup.startingLoc)
    mapScreen.windowManager.setTransition(0, 1.0f)
    setScreen(mapScreen)

    loadUserMainScript
  }

  def saveGame(slot: Int) = {
    assertOnBoundThread()
    mapScreen.persistPlayerLocation()
    SaveFile.write(persistent.toSerializable, project, slot)
  }

  def loadGame(slot: Int) = {
    assertOnBoundThread()
    val save = SaveFile.read(project, slot)
    assert(save.isDefined)
    persistent = new PersistentState(save.get)

    // Fix up menu and event enabled / disabled for legacy save games.
    if (persistent.hasInt(EVENTS_ENABLED))
      persistent.setInt(EVENTS_ENABLED, 1)
    if (persistent.hasInt(MENU_ENABLED))
      persistent.setInt(MENU_ENABLED, 1)

    setParty(persistent.getIntArray(PARTY))

    // Restore player location.
    setPlayerLoc(persistent.getLoc(PLAYER_LOC))

    mapScreen.windowManager.setTransition(0, 1.0f)
    setScreen(mapScreen)

    loadUserMainScript
  }

  def setPlayerLoc(loc: MapLoc) = {
    persistent.setLoc(PLAYER_LOC, loc)

    if (mapScreen != null)
      mapScreen.setPlayerLoc(loc)
  }

  def startBattle(encounterId: Int): Unit = {
    assertOnBoundThread()
    assert(encounterId >= 0)
    assert(encounterId < project.data.enums.encounters.length)

    val currentScreen = getScreen()
    if (currentScreen == battleScreen)
      return

    // Fade out map
    currentScreen.windowManager.setTransition(1, 0.6f)
    currentScreen.windowManager.runAfterTransition(() => {
      setScreen(battleScreen)
      battleScreen.windowManager.setTransition(0, 0.6f)

      val encounter = project.data.enums.encounters(encounterId)

      val battle = new Battle(
        project.data,
        persistent.getIntArray(PARTY),
        persistent.getPartyParameters(project.data.enums.characters),
        encounter,
        aiOpt = Some(new RandomEnemyAI))

      val (battleBackground, battleMusic) =
        mapScreen.mapAndAssetsOption.map { mapAndAssets =>
          (mapAndAssets.battleBackground, mapAndAssets.battleMusic)
        } getOrElse {
          ("", None)
        }
      battleScreen.startBattle(battle, battleBackground)
      if (!battleMusic.isEmpty) {
        battleScreen.playMusic(
          0,
          battleMusic,
          true,
          Transitions.fadeLength)
      }
    })
  }

  def setWindowskin(windowskinPath: String) = {
    battleScreen.windowManager.setWindowskin(windowskinPath)
    mapScreen.windowManager.setWindowskin(windowskinPath)
    startScreen.windowManager.setWindowskin(windowskinPath)
  }

  def quit() {
    assertOnBoundThread()

    Gdx.app.exit()
  }

  def gameOver() {
    battleScreen.reset()
    mapScreen.reset()
    startScreen.reset()
    // TODO: Ghetto but effective to just land on the start screen again.
    beginGame()
  }

  override def dispose() {
    assertOnBoundThread()

    battleScreen.dispose()
    mapScreen.dispose()

    if (atlasSprites != null)
      atlasSprites.dispose()

    assets.dispose()

    super.dispose()
  }
}