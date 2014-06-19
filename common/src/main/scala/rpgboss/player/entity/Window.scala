package rpgboss.player.entity

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.Color
import java.awt._
import java.awt.image._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Promise
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.player._

object Window {
  val Opening = 0
  val Open = 1
  val Closing = 2
  val Closed = 3

  val Left = 0
  val Center = 1
  val Right = 2
}

/**
 * This class is created and may only be accessed on the main GDX thread, with
 * the exception of the getScriptInterface(), which may only be used from
 * a different, scripting thread.
 */
class Window(
  manager: WindowManager,
  inputs: InputMultiplexer,
  x: Int, y: Int, 
  val w: Int, val h: Int,
  invisible: Boolean = false)
  extends InputHandler with ThreadChecked {
  
  def openCloseTime: Double = 0.25
  def initialState = if (openCloseTime > 0) Window.Opening else Window.Open 
  
  private var _state = initialState
  // Determines when states expire. In seconds.
  protected var stateAge = 0.0

  if (inputs != null)
    inputs.prepend(this)

  if (manager != null)
    manager.addWindow(this)

  /**
   * Accessed on multiple threads.
   */
  def state = synchronized {
    _state
  }
  
  def skin = manager.windowskin
  def skinRegion = manager.windowskinRegion

  private def changeState(newState: Int) = synchronized {
    assertOnBoundThread()
    _state = newState
    stateAge = 0.0
  }

  def update(delta: Float) = {
    assertOnBoundThread()
    stateAge += delta
    // change state of "expired" opening or closing animations
    if (stateAge >= openCloseTime) {
      state match {
        case Window.Opening => changeState(Window.Open)
        case Window.Open =>
        case Window.Closing => {
          changeState(Window.Closed)
        }
        case _ => Unit
      }
    }
  }

  def render(b: SpriteBatch): Unit = {
    assertOnBoundThread()
    
    if (invisible)
      return
    
    state match {
        case Window.Open => {
        skin.draw(b, skinRegion, x, y, w, h)
      }
      case Window.Opening => {
        val hVisible =
          math.max(32 + (stateAge / openCloseTime * (h - 32)).toInt, 32)

        skin.draw(b, skinRegion, x, y, w, hVisible)
      }
      case Window.Closing => {
        val hVisible =
          math.max(h - (stateAge / openCloseTime * (h - 32)).toInt, 32)

        skin.draw(b, skinRegion, x, y, w, hVisible)
      }
      case _ => Unit
    }
  }

  def startClosing(): Unit = {
    assertOnBoundThread()
    
    // This method may be called multiple times, but the subsequent calls after
    // the first should be ignored.
    if (state != Window.Opening && state != Window.Open) {
      return
    }
    
    changeState(Window.Closing)

    // We allow scripts to continue as soon as the window is closing to provide
    // a snappier game.
    closePromise.success(0)
  }

  class WindowScriptInterface {
    def getState() = {
      assertOnDifferentThread()
      state
    }
    
    def close() = {
      assertOnDifferentThread()
      GdxUtils.syncRun {
        startClosing()
      }
      awaitClose()
    }

    def awaitClose() = {
      assertOnDifferentThread()
      Await.result(closePromise.future, Duration.Inf)
    }
  }

  lazy val scriptInterface = new WindowScriptInterface

  def removeFromWindowManagerAndInputs() = {
    assertOnBoundThread()
    assert(state == Window.Closed)

    if (inputs != null)
      inputs.remove(this)

    if (manager != null)
      manager.removeWindow(this)
  }

  // This is used to either convey a choice, or simply that the window
  // has been closed
  private val closePromise = Promise[Int]()
}

/**
 * @param   stayOpenTime    If this is positive, window closes after it's open for
 *                          this period of time.
 */
class TextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  justification: Int = Window.Left,
  stayOpenTime: Double = 0.0)
  extends Window(manager, inputs, x, y, w, h) {
  
  def xpad = 24
  def ypad = 24

  def updateText(newText: Array[String]) = textImage.updateText(newText)

  val textImage = new WindowText(
    persistent,
    text,
    x + xpad,
    y + ypad,
    w - 2*xpad,
    h - 2*ypad,
    manager.fontbmp,
    justification)

  override def update(delta: Float) = {
    super.update(delta)
    textImage.update(delta)
    
    if (stayOpenTime > 0.0 && state == Window.Open && stateAge >= stayOpenTime)
      startClosing()
  }

  override def render(b: SpriteBatch) = {
    super.render(b)
    state match {
      case Window.Open => {
        textImage.render(b)
      }
      case Window.Opening => {
        textImage.render(b)
      }
      case _ => {
      }
    }
  }
}

class DamageTextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  damage: Int,
  initialX: Int, initialY: Int)
  // TODO: We pass 'null' as inputs here because we don't want to accept input.
  // Window has zeros for x, y, w, and h because the window itself is invisible.
  extends Window(manager, null, 0, 0, 0, 0, 
    invisible = true) {
  
  private val expiryTime = 1.6
  private val yDisplacement = -30.0
  
  private var age = 0.0
  
  val textImage = new WindowText(
    persistent,
    initialText = Array(damage.toString()),
    x = initialX,
    y = initialY,
    w = 20,
    h = 20,
    fontbmp = manager.fontbmp,
    justification = Window.Center)

  override def update(delta: Float): Unit = {
    super.update(delta)
    
    if (state != Window.Open)
      return
    
    age += delta;
    
    textImage.updatePosition(
        initialX, 
        ((age / expiryTime * yDisplacement) + initialY).toInt)
    
    super.update(delta)
    textImage.update(delta)
    
    if (age > expiryTime) {
      startClosing()
    }
  }

  override def render(b: SpriteBatch) = {
    super.render(b)
    textImage.render(b)
  }
}


class PrintingTextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  timePerChar: Float,
  linesPerBlock: Int = 4,
  justification: Int = Window.Left)
  extends Window(manager, inputs, x, y, w, h) {
  val xpad = 24
  val ypad = 24

  val textImage = new PrintingWindowText(
    persistent,
    text,
    x + xpad,
    y + ypad,
    w - 2*xpad,
    h - 2*ypad,
    skin,
    skinRegion,
    manager.fontbmp,
    timePerChar,
    linesPerBlock,
    justification)

  override def keyDown(key: Int): Unit = {
    import MyKeys._
    if (state == Window.Closing || state == Window.Closed)
      return

    if (key == OK) {
      if (textImage.allTextPrinted)
        startClosing()
      else if(textImage.wholeBlockPrinted)
        textImage.advanceBlock()
      else
        textImage.speedThrough()
    }
  }

  override def update(delta: Float) = {
    super.update(delta)
    textImage.update(delta)
  }

  override def render(b: SpriteBatch) = {
    super.render(b)
    state match {
      case Window.Open => {
        textImage.render(b)
      }
      case Window.Opening => {
        textImage.render(b)
      }
      case _ => {
      }
    }
  }
}
