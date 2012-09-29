package rpgboss.player

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * This class renders stuff on the screen.
 * 
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class ScreenLayer(game: MyGame, state: GameState) {
  def project = game.project
  val batch = new SpriteBatch()
  val shapeRenderer = new ShapeRenderer()
  
  val windowskin = Windowskin.readFromDisk(project, project.data.windowskin)
  val windowskinTexture = 
    new Texture(Gdx.files.absolute(windowskin.dataFile.getAbsolutePath()))
  val windowskinRegion = new TextureRegion(windowskinTexture)
  
  val font = Msgfont.readFromDisk(project, project.data.msgfont)
  var fontbmp : BitmapFont = font.getBitmapFont()

  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, 640, 480) // y points down
  screenCamera.update()
  
  batch.setProjectionMatrix(screenCamera.combined)
  batch.enableBlending()
  batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
  shapeRenderer.setProjectionMatrix(screenCamera.combined)
  
  def update() = {
    
  }
  
  def render() = {
    /*
     * We define our screen coordinates to be 640x480.
     * This is the easiest thing possible.
     * 
     * This definitely favors 4:3 aspect ratios, but that's the historic
     * JRPG look, and I'm not sure how to support varying aspect ratios without
     * really complicating the code...
     */
    
    batch.begin()
    
    // Render pictures
    for(pic <- state.pictures) {
      if(pic != null) {
        pic.render(batch)
      }
    }
    
    // Render all windows
    if(!state.windows.isEmpty) 
      state.windows.head.update(true)
    if(state.windows.length > 1)
      state.windows.tail.foreach(_.update(false))
    state.windows.foreach(_.render(batch))
    
    batch.end()
    
    // Render transition
    state.curTransition.synchronized {
      state.curTransition map { transition =>
        
        // Spritebatch seems to turn off blending after it's done. Turn it on.
        Gdx.gl.glEnable(GL10.GL_BLEND)
        shapeRenderer.begin(ShapeRenderer.ShapeType.FilledRectangle)
        
        shapeRenderer.setColor(0, 0, 0, transition.curAlpha)
        shapeRenderer.filledRect(0, 0, 640, 480)
        shapeRenderer.end()
      }
    }
  }
  
  def dispose() = {
    batch.dispose()
  }
}