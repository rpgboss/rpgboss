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
import akka.dispatch.{ExecutionContext}
import java.util.concurrent.Executors
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import rpgboss.model.resource.RpgAssetManager

class MutableMapLoc(var map: Int = -1, var x: Float = 0, var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)
  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

object Global {
  val pool = Executors.newCachedThreadPool()

  implicit val ec = ExecutionContext.fromExecutorService(pool)
}

class MyGame(gamepath: File) extends ApplicationListener {
  val project = Project.readFromDisk(gamepath).get
  
  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger() 
  
  var mapLayer: Option[MapLayer] = None
  var screenLayer: ScreenLayer = null
  val inputs = new MyInputMultiplexer()
  
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
  // Where in the "second" we are. Varies from 0 to 1.0
  var accumDelta : Float = 0.0f
  
  val state = new GameState(this, project)
  
  val assets = new RpgAssetManager(project)
  
  def create() = {
    // Attach inputs
    Gdx.input.setInputProcessor(inputs)
    
    screenLayer = new ScreenLayer(this, state)
    
    ScriptThread(this, "main.js", "main()").run()
  }
  
  override def dispose() {
    mapLayer.map(_.dispose())
    screenLayer.dispose()
  }
  
  override def pause() {}
  
  override def render() {
    import Tileset._
    
    val delta = Gdx.graphics.getDeltaTime()
    // Set delta time
    accumDelta = (accumDelta + delta)% 1.0f
    
    // Log fps
    fps.log()
    
    // update state
    state.update()
    
    mapLayer.map(_.update())
    screenLayer.update()
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL10.GL_BLEND)
    
    // Render the two layers
    mapLayer.map(_.render())
    screenLayer.render()
  }
  
  
  override def resize(x: Int, y: Int) {}
  override def resume() {}

}