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

/**
 * This class renders stuff on the screen.
 * 
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 */
class ScreenLayer(game: MyGame) {
  def project = game.project
  def batch = game.batch
  
  val windowskin = Windowskin.readFromDisk(project, project.data.windowskin)
  val windowskinTexture = 
    new Texture(Gdx.files.absolute(windowskin.dataFile.getAbsolutePath()))
  val windowskinRegion = new TextureRegion(windowskinTexture)
  
  val font = Msgfont.readFromDisk(project, project.data.msgfont)
  var fontbmp : BitmapFont = font.getBitmapFont()

  var windows = collection.mutable.Stack[Window]()
  
  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, 640, 480) // y points down
  screenCamera.update()

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
    
    batch.setProjectionMatrix(screenCamera.combined)
    
    windows.head.update(true)
    windows.tail.foreach(_.update(false))
    windows.foreach(_.render(batch))
  }
  
  def dispose() = {
    
  }
}