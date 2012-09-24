/**
 *
 */
package rpgboss.player

import com.badlogic.gdx.Screen
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.FPSLogger
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL10
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont

/**
 * @author tommycli
 *
 */
class GameStartScreen(game: MyGame) extends Screen {
  val project = game.project
  
  val windowskin = Windowskin.readFromDisk(project, project.data.windowskin)
  val windowskinTexture = 
    new Texture(Gdx.files.absolute(windowskin.dataFile.getAbsolutePath()))
  val windowskinRegion = new TextureRegion(windowskinTexture)
  
  val font = Msgfont.readFromDisk(project, project.data.msgfont)
  val fontbmp : BitmapFont = font.getBitmapFont()
  
  val fps = new FPSLogger() 
  val batch = new SpriteBatch()
  
  val camera: OrthographicCamera = new OrthographicCamera()
  camera.setToOrtho(true, 640, 480) // y points down
  camera.update()
  
  var windows = collection.mutable.Stack[Window]()
  
  {
    val winW = 200
    windows.push(new ChoiceWindow(project,
       Array(
        "New Game", 
        "Load Game", 
        "Quit"),
       320-(winW/2), 280, winW, 130,
       windowskin, windowskinRegion, fontbmp,
       state = Window.Opening,
       framesPerChar = 0,
       justification = Window.Center)
    )
  }
  
  def render(delta: Float): Unit = {
    /*
     * We define our screen coordinates to be 640x480.
     * This is the easiest thing possible.
     * 
     * This definitely favors 4:3 aspect ratios, but that's the historic
     * JRPG look, and I'm not sure how to support varying aspect ratios without
     * really complicating the code...
     */
    
    // Log fps
    fps.log()
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    
    batch.setProjectionMatrix(camera.combined)
    batch.begin()
    
    windows.head.update(true)
    windows.tail.foreach(_.update(false))
    windows.foreach(_.render(batch))
    //windowskin.draw(batch, windowskinRegion, 0, 200, 200, 64)
    
    batch.end()
  }

  def resize(w: Int, h: Int): Unit = {}

  def show(): Unit = {}

  def hide(): Unit = {}

  def pause(): Unit = {}

  def resume(): Unit = {}

  def dispose(): Unit = {}

}