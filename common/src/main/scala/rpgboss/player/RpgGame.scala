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

trait RpgScreen {
  def windowManager: WindowManager
}

class RpgGame(gamepath: File) extends ApplicationListener {
  val project = Project.readFromDisk(gamepath).get

  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger()

  var mapScreen: MapScreen = null
  var battleScreen: BattleScreen = null
  val inputs = new InputMultiplexer()

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

    // Attach inputs
    Gdx.input.setInputProcessor(inputs)

    atlasSprites = GdxUtils.generateSpritesTextureAtlas(spritesets.values)

    persistent = new PersistentState()

    // TODO: Make configurable screen pixel dimensions
    battleScreen = new BattleScreen(project, 640, 480)
    mapScreen = new MapScreen(RpgGame.this, 640, 480)

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

  override def dispose() {
    battleScreen.dispose()
    mapScreen.dispose()
    atlasSprites.dispose()
  }

  override def pause() {}

  override def render() {
    import Tileset._

    val delta = Gdx.graphics.getDeltaTime()

    // Log fps
    //fps.log()

    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL20.GL_BLEND)

    if (assets.update()) {
      // update state
      if (battleScreen.battleActive) {
        battleScreen.update(delta)
        battleScreen.render(atlasSprites)
      } else {
        mapScreen.update(delta)
        mapScreen.render()
      }
    } else {
      // TODO: loading screen
    }
  }

  override def resize(x: Int, y: Int) {}
  override def resume() {}
}