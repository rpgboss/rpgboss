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
  with HasScriptConstants {
  val project = Project.readFromDisk(gamepath).get

  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger()

  var mapScreen: MapScreen = null
  var battleScreen: BattleScreen = null

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

  def create() = {
    com.badlogic.gdx.utils.Timer.instance().start()

    atlasSprites = GdxUtils.generateSpritesTextureAtlas(spritesets.values)

    persistent = new PersistentState()

    // TODO: Make configurable screen pixel dimensions
    battleScreen = 
      new BattleScreen(Some(this), assets, atlasSprites, project, 640, 480)
    mapScreen = new MapScreen(this, 640, 480)

    setScreen(mapScreen)

    // Register accessors
    TweenAccessors.registerAccessors()

    beginGame()
  }

  def beginGame() = {
    ScriptThread.fromFile(
      RpgGame.this,
      mapScreen.scriptInterface,
      "main.js",
      "main()").run()
  }
  
  /**
   * Sets the members of the player's party. Controls the sprite for both
   * walking on the map, as well as the party members in a battle.
   *
   * TODO: Figure out if this requires @partyArray to be non-empty.
   */
  def setParty(partyArray: Array[Int]) = {
    persistent.setIntArray(PARTY, partyArray)

    if (mapScreen != null) {
      if (partyArray.length > 0) {
        val spritespec = project.data.enums.characters(partyArray(0)).sprite
        mapScreen.playerEntity.setSprite(spritespec)
      } else {
        mapScreen.playerEntity.setSprite(None)
      }
    }
  }
  
  def startNewGame() = {
    setParty(project.data.startup.startingParty.toArray)
    // Initialize data structures

    var characters = project.data.enums.characters.toArray;
    persistent.setStringArray(CHARACTER_NAMES, characters.map(_.name));

    persistent.setIntArray(CHARACTER_LEVELS, characters.map(_.initLevel))

    val characterStats = for (c <- characters)
      yield BattleStats(project.data, c.baseStats(project.data, c.initLevel),
                        c.startingEquipment)

    persistent.setIntArray(CHARACTER_HPS, characterStats.map(_.mhp))
    persistent.setIntArray(CHARACTER_MPS, characterStats.map(_.mmp))
    persistent.setIntArray(CHARACTER_MAX_HPS, characterStats.map(_.mhp))
    persistent.setIntArray(CHARACTER_MAX_MPS, characterStats.map(_.mmp))

    persistent.setIntArray(CHARACTER_ROWS, characters.map(x => 0))
  }

  override def dispose() {
    super.dispose()
    
    battleScreen.dispose()
    mapScreen.dispose()
    atlasSprites.dispose()
  }
}