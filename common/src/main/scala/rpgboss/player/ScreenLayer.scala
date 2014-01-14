package rpgboss.player

import rpgboss.model._
import rpgboss.model.Constants._
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
 * 
 * This class should only be accessed on the gdx thread
 */
class ScreenLayer(project: Project, screenW: Int, screenH: Int) {
  val batch = new SpriteBatch()
  val shapeRenderer = new ShapeRenderer()
  
  // Should only be modified on the Gdx thread
  var curTransition: Option[Transition] = None

  val windowskin = 
    Windowskin.readFromDisk(project, project.data.startup.windowskin)
  val windowskinTexture =
    new Texture(Gdx.files.absolute(windowskin.dataFile.getAbsolutePath()))
  val windowskinRegion = new TextureRegion(windowskinTexture)

  val font = Msgfont.readFromDisk(project, project.data.startup.msgfont)
  var fontbmp: BitmapFont = font.getBitmapFont()
  
  private var lastWindowId: Long = 0
  
  val pictures = Array.fill[Option[PictureInfo]](64)(None)
  val windows = new collection.mutable.ArrayBuffer[Window]
  
  def getWindowId() : Long = {
    lastWindowId += 1
    lastWindowId
  }

  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, screenW, screenH) // y points down
  screenCamera.update()

  batch.setProjectionMatrix(screenCamera.combined)
  batch.enableBlending()
  batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
  shapeRenderer.setProjectionMatrix(screenCamera.combined)
  
  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int, h: Int) = {
    // TODO: Remove this Pot hack
    GLTexture.setEnforcePotImages(false)
    val picture = Picture.readFromDisk(project, name)
    showTexture(slot, picture.newGdxTexture, x, y, w, h)
  }

  def showTexture(slot: Int, texture: Texture, x: Int, y: Int, w: Int, 
                  h: Int) = {
    // TODO: Remove this Pot hack
    GLTexture.setEnforcePotImages(false)
    pictures(slot).map(_.dispose())
    pictures(slot) = Some(PictureInfo(texture, x, y, w, h))
  }
  
  def hidePicture(slot: Int) = {
    pictures(slot).map(_.dispose())
    pictures(slot) = None
  }

  def update(delta: Float) = {
    // Update windows
    if (!windows.isEmpty)
      windows.head.update(delta, true)
    if (windows.length > 1)
      windows.tail.foreach(_.update(delta, false))

  }
  
  // Render that's called before the map layer is drawn
  def preMapRender() = {
    batch.begin()
    
    for (i <- PictureSlots.BELOW_MAP until PictureSlots.ABOVE_MAP;
         pic <- pictures(i)) {
      pic.render(batch)
    } 
    
    batch.end()
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

    for (i <- PictureSlots.ABOVE_MAP until PictureSlots.ABOVE_WINDOW;
         pic <- pictures(i)) {
      pic.render(batch)
    }

    // Render all windows
    windows.foreach(_.render(batch))

    for (i <- PictureSlots.ABOVE_WINDOW until PictureSlots.END;
         pic <- pictures(i)) {
      pic.render(batch)
    }
    
    batch.end()

    // Render transition
    curTransition map { transition =>

      // Spritebatch seems to turn off blending after it's done. Turn it on.
      Gdx.gl.glEnable(GL10.GL_BLEND)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

      shapeRenderer.setColor(0, 0, 0, transition.curAlpha.toFloat)
      shapeRenderer.rect(0, 0, 640, 480)
      shapeRenderer.end()
    }
  }

  def dispose() = {
    batch.dispose()
    for (pictureOpt <- pictures; picture <- pictureOpt) {
      picture.dispose()
    }
  }
}

/**
 * Need call on dispose first
 */
case class PictureInfo(
  texture: Texture,
  x: Int, y: Int, w: Int, h: Int) {

  def dispose() = texture.dispose()

  def render(batch: SpriteBatch) = {
    batch.draw(texture,
      x, y, w, h,
      0, 0, texture.getWidth(), texture.getHeight(),
      false, true)
  }
}