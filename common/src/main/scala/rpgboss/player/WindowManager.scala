package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.lib.ThreadChecked
import rpgboss.lib.Rect
import rpgboss.lib.Layout
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable.MutableList

/**
 * This class renders stuff on the screen.
 *
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 *
 * This class should only be accessed on the gdx thread
 */
class WindowManager(
  val assets: RpgAssetManager,
  val project: Project,
  val screenW: Int,
  val screenH: Int) extends ThreadChecked with LazyLogging {
  val batch = new SpriteBatch()
  val shapeRenderer = new ShapeRenderer()

  // Should only be modified on the Gdx thread
  /**
   * Should start all black.
   */
  var transitionAlpha = 1.0f
  val transitionTweener =
    new FloatTweener(() => transitionAlpha, transitionAlpha = _)

  val tintColor = new Color
  val tintTweener = new Tweener[Color] {
    var _startValue = new Color()
    var _endValue = new Color()

    def get() = tintColor
    def set(newValue: Color) = tintColor.set(newValue)
    def interpolate(startValue: Color, endValue: Color, alpha: Float) = {
      val newColor = new Color(startValue)
      newColor.lerp(endValue, alpha)
      newColor
    }
  }

  val windowskin =
    Windowskin.readFromDisk(project, project.data.startup.windowskin)
  val windowskinTexture = new Texture(windowskin.getGdxFileHandle)
  val windowskinRegion = new TextureRegion(windowskinTexture)

  val font = Msgfont.readFromDisk(project, project.data.startup.msgfont)
  var fontbmp: BitmapFont = font.getBitmapFont()

  val pictures = Array.fill[Option[PictureLike]](64)(None)
  private val windows = new collection.mutable.ArrayBuffer[Window]

  def setTransition(endAlpha: Float, duration: Float) = {
    assertOnBoundThread()
    transitionTweener.tweenTo(endAlpha, duration)
  }

  /**
   * @param   closure   Runs on the Gdx thread once the current transition is
   *                    over. Runs immediately if there is no current
   *                    transition.
   */
  def runAfterTransition(closure: () => Unit) {
    transitionTweener.runAfterDone(closure)
  }

  def finishTransition() = {
    assertOnBoundThread()
    transitionTweener.finish()
  }

  def inTransition = !transitionTweener.done

  // TODO: Investigate if a more advanced z-ordering is needed other than just
  // putting the last-created one on top.
  def addWindow(window: Window) = {
    assertOnBoundThread()
    windows.prepend(window)
  }
  def removeWindow(window: Window) = {
    assertOnBoundThread()
    windows -= window
  }
  def focusWindow(window: Window) = {
    assertOnBoundThread()
    removeWindow(window)
    addWindow(window)
  }

  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, screenW, screenH) // y points down
  screenCamera.update()

  batch.setProjectionMatrix(screenCamera.combined)
  batch.enableBlending()
  batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
  shapeRenderer.setProjectionMatrix(screenCamera.combined)

  def showPictureByName(slot: Int, name: String, layout: Layout, alpha:Float=1.0f) = {
    assertOnBoundThread()
    logger.debug("showPictureByName(%d, %s, %s)".format(slot, name, layout))

    val picture = Picture.readFromDisk(project, name)
    showPicture(slot, TexturePicture(assets, picture, layout, alpha))
  }

  def showPicture(slot: Int, newPicture: PictureLike): Unit = {
    assertOnBoundThread()
    pictures(slot).map(_.dispose())
    pictures(slot) = Some(newPicture)
  }

  def hidePicture(slot: Int) = {
    pictures(slot).map(_.dispose())
    pictures(slot) = None
  }

  def reset() = {
    for (i <- 0 until pictures.length) {
      hidePicture(i)
    }
    tintColor.set(0, 0, 0, 0)

    windows.foreach(_.startClosing())

    // TODO: This could potentially leave window promises unfulfilled, since
    // we don't update them anymore.
    windows.clear()
  }

  def update(delta: Float) = {
    transitionTweener.update(delta)
    tintTweener.update(delta)
    windows.foreach(_.update(delta))

    // TODO: Avoid a memory alloc here
    val toRemove = windows.filter(_.state == Window.Closed)
    toRemove.foreach(_.removeFromWindowManagerAndInputs())
  }

  // Render that's called before the map layer is drawn
  def preMapRender() = {
    batch.begin()

    for (i <- PictureSlots.BELOW_MAP until PictureSlots.ABOVE_MAP;
         pic <- pictures(i)) {
      pic.render(this, batch)
    }

    batch.end()
  }

  var screenTextArray = MutableList[ScreenText]()

  def addDrawText(text: ScreenText):Boolean = {
    screenTextArray.foreach { text2:ScreenText =>
      if(text2.id == text.id) {
        removeDrawText(text.id)
      }
    }
    screenTextArray += text
    
    return true
  }

  def removeDrawText(id: Int):Boolean = {
    var removedSomething:Boolean = false
    var newTextArray = MutableList[ScreenText]()
    screenTextArray.foreach { text:ScreenText =>
      if(text.id!=id){
        newTextArray += text
      } else {
        removedSomething = true
      }
    }
    screenTextArray = newTextArray

    return removedSomething
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
      pic.render(this, batch)
    }

    screenTextArray.foreach { text:ScreenText =>
      text.render(this,batch)
    }

    // Render all windows
    windows.reverseIterator.foreach(_.render(batch))

    for (i <- PictureSlots.ABOVE_WINDOW until PictureSlots.END;
         pic <- pictures(i)) {
      pic.render(this, batch)
    }

    batch.end()

    // Render transition
    if (transitionAlpha != 0 || tintColor.a != 0) {
      // Spritebatch seems to turn off blending after it's done. Turn it on.
      Gdx.gl.glEnable(GL20.GL_BLEND)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

      shapeRenderer.setColor(tintColor)
      shapeRenderer.rect(0, 0, screenW, screenH)

      shapeRenderer.setColor(0, 0, 0, transitionAlpha)
      shapeRenderer.rect(0, 0, screenW, screenH)

      shapeRenderer.end()
    }
  }

  def dispose() = {
    assertOnBoundThread()
    batch.dispose()
    for (pictureOpt <- pictures; picture <- pictureOpt) {
      picture.dispose()
    }
  }
}

trait PictureLike {
  def dispose()
  def render(manager: WindowManager, batch: SpriteBatch)
}

/**
 * Need call on dispose first
 */
case class TexturePicture[MT <: AnyRef](
  assets: RpgAssetManager, resource: ImageResource[_, MT],
  layout: Layout, alpha:Float=1.0f) extends PictureLike {

  resource.loadAsset(assets)
  def dispose() = resource.dispose(assets)

  override def render(manager: WindowManager, batch: SpriteBatch) = {
    if (resource.isLoaded(assets)) {
      val texture = resource.getAsset(assets)
      val rect = layout.getRect(texture.getWidth(), texture.getHeight(),
                                manager.screenW, manager.screenH)
      var c = batch.getColor();
      batch.setColor(c.r, c.g, c.b, alpha);
      batch.draw(texture,
        rect.left, rect.top, rect.w, rect.h,
        0, 0, texture.getWidth(), texture.getHeight(),
        false, true)
      batch.setColor(c.r, c.g, c.b, 1);
    }
  }
}

case class TextureAtlasRegionPicture(
  atlasSprites: TextureAtlas,
  regionName: String,
  x: Float, y: Float, w: Float, h: Float,
  srcX: Int, srcY: Int, srcW: Int, srcH: Int) extends PictureLike {

  val region = atlasSprites.findRegion(regionName)
  val srcXInRegion = region.getRegionX() + srcX
  val srcYInRegion = region.getRegionY() + srcY

  def dispose() = {
    // No need to dispose since the texture is part of the TextureAtlas
  }

  override def render(manager: WindowManager, batch: SpriteBatch) = {
    batch.draw(
      region.getTexture(),
      x, y, w, h,
      srcXInRegion,
      srcYInRegion,
      srcW,
      srcH,
      false, true)
  }
}