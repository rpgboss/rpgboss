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

class MyGame(gamepath: File)
  extends ApplicationListener {
  val project = Project.readFromDisk(gamepath).get

  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger()

  var mapLayer: MapLayer = null
  var screenLayer: ScreenLayer = null
  val inputs = new MyInputMultiplexer()
  
  /**
   * Run the following on the GUI thread
   */
  def syncRun(op: => Any) = {
    val runnable = new Runnable() {
      def run() = op
    }
    Gdx.app.postRunnable(runnable)
  }

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

  var state: GameState = null

  val assets = new RpgAssetManager(project)

  def create() = {
    com.badlogic.gdx.utils.Timer.instance().start()
    
    // Attach inputs
    Gdx.input.setInputProcessor(inputs)

    state = new GameState(this, project)
    mapLayer = new MapLayer(this)
    screenLayer = new ScreenLayer(this, state)

    // Register accessors
    TweenAccessors.registerAccessors()
    
    ScriptThread.fromFile(this, "main.js", "main()").run()
  }

  override def dispose() {
    state.dispose()
    mapLayer.dispose()
    screenLayer.dispose()
  }

  override def pause() {}

  override def render() {
    import Tileset._

    val delta = Gdx.graphics.getDeltaTime()

    // Log fps
    //fps.log()
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL10.GL_BLEND)

    if(assets.update()) {
      // update state
      state.update(delta)
  
      mapLayer.update(delta)
      screenLayer.update(delta)
      
      // Render the two layers
      mapLayer.render()
      screenLayer.render()
    } else {
      // TODO: loading screen
    }
  }

  override def resize(x: Int, y: Int) {}
  override def resume() {}
}