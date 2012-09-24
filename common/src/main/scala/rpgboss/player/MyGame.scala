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

class MutableMapLoc(var map: Int = -1, var x: Float = 0, var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)
  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

class MyGame(gamepath: File) extends ApplicationListener {
  val project = Project.readFromDisk(gamepath).get
  
  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger() 
  
  var mapLayer : MapLayer = null
  var screenLayer: ScreenLayer = null
  
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
  var batch: SpriteBatch = null
  
  // Where in the "second" we are. Varies from 0 to 1.0
  var accumDelta : Float = 0.0f
  
  // Other creation stuff that was formerly in create()
  def create() = {
    batch = new SpriteBatch()
    
    mapLayer = new MapLayer(this)
    //screenLayer = new ScreenLayer(this)
  }
  
  override def dispose() {
    mapLayer.dispose()
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
    
    
    mapLayer.update()
    //screenLayer.update()
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    
    batch.begin()
    mapLayer.render()
    //screenLayer.render()
        
    batch.end()
  }
  
  
  override def resize(x: Int, y: Int) {}
  override def resume() {}

}