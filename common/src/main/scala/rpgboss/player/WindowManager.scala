package rpgboss.player

import scala.collection.mutable.MutableList
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.lib.Layout
import rpgboss.lib.ThreadChecked
import rpgboss.model.PictureSlots
import rpgboss.model.Project
import rpgboss.model.resource.ImageResource
import rpgboss.model.resource.Msgfont
import rpgboss.model.resource.Picture
import rpgboss.model.resource.RpgAssetManager
import rpgboss.model.resource.Windowskin
import rpgboss.player.entity.Window
import rpgboss.lib.BoxLike
import rpgboss.lib.Rect

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

  // Should only be modified on the Gdx thread
  val animationManager = new AnimationManager(screenW, screenH)

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

  var windowskin: Windowskin = null
  var windowskinTexture: Texture = null

  val font = Msgfont.readFromDisk(project, project.data.startup.msgfont)

  def fontbmp = {
    if (_fontbmp == null)
      updateBitmapFont("")
    _fontbmp
  }

  var _fontbmp: BitmapFont = null
  def updateBitmapFont(distinctChars: String) = {
    _fontbmp = font.getBitmapFont(distinctChars)
  }

  val pictures = Array.fill[Option[PictureLike]](164)(None)
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

  def setWindowskin(windowskinPath: String) = {
    if (windowskinTexture != null)
      windowskinTexture.dispose()

    windowskin = Windowskin.readFromDisk(project, windowskinPath)
    windowskinTexture = new Texture(windowskin.getGdxFileHandle)
  }
  setWindowskin(project.data.startup.windowskin)

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

  def showPictureByName(slot: Int, name: String, layout: Layout,
      alpha: Float = 1.0f) = {
    assertOnBoundThread()
    val picture = Picture.readFromDisk(project, name)
    showPicture(slot, new TexturePicture(assets, picture, layout, alpha))
  }

  def showPictureLoop(slot: Int, folderPath: String, layout: Layout,
      alpha: Float = 1.0f, framesPerSecond: Int = 30) = {
    assertOnBoundThread()
    val filesUnderPath = Picture.listResourcesUnderPath(project, folderPath)

    if (!filesUnderPath.isEmpty) {
      val pictureArray = filesUnderPath.map(picturePath => {
        val picture = Picture.readFromDisk(project, picturePath)
        new TexturePicture(assets, picture, layout, alpha)
      })
      showPicture(slot, new PictureSequence(
          pictureArray, loop = true, framesPerSecond))
    }
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
    pictures.foreach(_.map(_.update(delta)))

    // TODO: Avoid a memory alloc here
    val toRemove = windows.filter(_.state == Window.Closed)
    toRemove.foreach(_.removeFromWindowManagerAndInputs())

    animationManager.update(delta)
  }

  // Render that's called before the map layer is drawn
  def preMapRender(batch: SpriteBatch, screenCamera: OrthographicCamera) = {
    batch.begin()

    batch.setProjectionMatrix(screenCamera.combined)
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

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

  def render(batch: SpriteBatch, shapeRenderer: ShapeRenderer,
      screenCamera: OrthographicCamera) = {
    batch.setProjectionMatrix(screenCamera.combined)
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    shapeRenderer.setProjectionMatrix(screenCamera.combined)

    /*
     * We define our screen coordinates to be 640x480.
     * This is the easiest thing possible.
     *
     * This definitely favors 4:3 aspect ratios, but that's the historic
     * JRPG look, and I'm not sure how to support varying aspect ratios without
     * really complicating the code...
     */

    if (tintColor.a != 0) {
      // Spritebatch seems to turn off blending after it's done. Turn it on.
      Gdx.gl.glEnable(GL20.GL_BLEND)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

      shapeRenderer.setColor(tintColor)
      shapeRenderer.rect(0, 0, screenW, screenH)

      shapeRenderer.end()
    }

    batch.begin()

    for (i <- PictureSlots.ABOVE_MAP until PictureSlots.ABOVE_WINDOW;
         pic <- pictures(i)) {
      pic.render(this, batch)
    }

    screenTextArray.foreach { text:ScreenText =>
      text.render(this,batch)
    }

    batch.end()

    animationManager.render(batch, shapeRenderer, screenCamera)

    batch.begin()

    // Render all windows
    windows.reverseIterator.foreach(_.render(batch))

    for (i <- PictureSlots.ABOVE_WINDOW until PictureSlots.END;
         pic <- pictures(i)) {
      pic.render(this, batch)
    }

    batch.end()

    // Render transition
    if (transitionAlpha != 0) {
      // Spritebatch seems to turn off blending after it's done. Turn it on.
      Gdx.gl.glEnable(GL20.GL_BLEND)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

      shapeRenderer.setColor(0, 0, 0, transitionAlpha)
      shapeRenderer.rect(0, 0, screenW, screenH)

      shapeRenderer.end()
    }
  }

  def dispose() = {
    assertOnBoundThread()
    for (pictureOpt <- pictures; picture <- pictureOpt) {
      picture.dispose()
    }
    windowskinTexture.dispose()
    animationManager.dispose()
  }
}

trait PictureLike {
  def dispose()
  def update(delta: Float)
  def render(manager: WindowManager, batch: SpriteBatch)
  def setAnimationTint(color: Color)
  def getRect(): Rect
}

/**
 * Need call on dispose first
 */
class TexturePicture[MT <: AnyRef](
  assets: RpgAssetManager, resource: ImageResource[_, MT],
  layout: Layout, alpha: Float = 1.0f) extends PictureLike {

  resource.loadAsset(assets)
  def dispose() = resource.dispose(assets)

  private var _rect: Option[Rect] = None

  val animationTint = Color.WHITE.cpy()
  def setAnimationTint(color: Color) = animationTint.set(color)
  override def getRect() = _rect.getOrElse(Rect(0, 0, 100, 100))

  override def update(delta: Float) = {}

  override def render(manager: WindowManager, batch: SpriteBatch) = {
    if (resource.isLoaded(assets)) {
      val texture = resource.getAsset(assets)
      val rect = layout.getRect(texture.getWidth(), texture.getHeight(),
                                manager.screenW, manager.screenH)
      _rect = Some(rect)
      batch.draw(texture,
        rect.left, rect.top, rect.w, rect.h,
        0, 0, texture.getWidth(), texture.getHeight(),
        false, true)

      val modifiedAnimationTint = animationTint.cpy()
      modifiedAnimationTint.a *= alpha
      batch.setColor(modifiedAnimationTint);
      batch.draw(texture,
        rect.left, rect.top, rect.w, rect.h,
        0, 0, texture.getWidth(), texture.getHeight(),
        false, true)

      batch.setColor(Color.WHITE)
    }
  }
}

class PictureSequence[T <: PictureLike](
  pictures: Array[T], loop: Boolean = false, framesPerSecond: Int = 30)
  extends PictureLike {
  assert(!pictures.isEmpty)

  var _time = 0.0f

  def timePerFrame = 1.0f / framesPerSecond.toFloat
  val loopTime = timePerFrame * pictures.length

  def getCurFrame() = (_time / timePerFrame).toInt % pictures.length

  def dispose() = pictures.foreach(_.dispose())
  def update(delta: Float) = {
    _time += delta

    pictures.foreach(_.update(delta))
  }
  def render(manager: WindowManager, batch: SpriteBatch): Unit = {
    if (!loop && _time > loopTime)
      return

    pictures(getCurFrame()).render(manager, batch)
  }
  def setAnimationTint(color: Color) = {
    pictures.foreach(_.setAnimationTint(color))
  }
  def getRect(): Rect = {
    pictures.head.getRect()
  }
}

/**
 * @param   x     Specifies the left the destination X in screen coordinates.
 */
class TextureAtlasRegionPicture(
  atlasSprites: TextureAtlas,
  regionName: String,
  x: Float, y: Float, w: Float, h: Float,
  srcX: Int, srcY: Int, srcW: Int, srcH: Int) extends PictureLike {

  val region = atlasSprites.findRegion(regionName)
  val srcXInRegion = region.getRegionX() + srcX
  val srcYInRegion = region.getRegionY() + srcY

  val animationTint = Color.WHITE.cpy()
  def setAnimationTint(color: Color) = animationTint.set(color)

  override def getRect() = Rect(x + w / 2, y + h / 2, w, h)

  def dispose() = {
    // No need to dispose since the texture is part of the TextureAtlas
  }

  override def update(delta: Float) = {}

  override def render(manager: WindowManager, batch: SpriteBatch) = {
    batch.draw(
      region.getTexture(),
      x, y, w, h,
      srcXInRegion,
      srcYInRegion,
      srcW,
      srcH,
      false, true)
    batch.setColor(animationTint)
    batch.draw(
      region.getTexture(),
      x, y, w, h,
      srcXInRegion,
      srcYInRegion,
      srcW,
      srcH,
      false, true)
    batch.setColor(Color.WHITE)
  }
}