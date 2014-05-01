package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import java.awt._
import java.awt.image._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.assets.AssetManager
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
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

// stateAge starts at 0 and goes up as window opens or closes
class Window(
  manager: WindowManager,
  inputs: InputMultiplexer,
  val x: Int, val y: Int, val w: Int, val h: Int,
  initialState: Int = Window.Opening,
  openCloseTime: Double = 0.25)
  extends InputHandler {
  private var _state = initialState
  // Determines when states expire. In seconds.
  var stateAge = 0.0
  
  def state = _state
  def skin = manager.windowskin
  def skinRegion = manager.windowskinRegion
  
  def changeState(newState: Int) = {
    _state = newState
    stateAge = 0.0
  }

  def update(delta: Float) = {
    stateAge += delta
    // change state of "expired" opening or closing animations
    if (stateAge >= openCloseTime) {
      _state match {
        case Window.Opening => changeState(Window.Open)
        case Window.Open =>
        case Window.Closing => {
          postClose()
          changeState(Window.Closed)
        }
        case _ => Unit
      }
    }
  }

  def render(b: SpriteBatch) = _state match {
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

  def close() = {
    if (_state != Window.Closing && _state != Window.Closed)
      changeState(Window.Closing)
  }

  def postClose() = {
    closePromise.success(0)
  }

  def awaitClose() = {
    Await.result(closePromise.future, Duration.Inf)
  }

  def destroy() = {
    close()

    awaitClose()

    inputs.remove(this)
    manager.removeWindow(this)
  }

  // This is used to either convey a choice, or simply that the window
  // has been closed
  private val closePromise = Promise[Int]()
}

class TextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  initialState: Int = Window.Opening,
  openCloseTime: Double = 0.25,
  justification: Int = Window.Left)
  extends Window(manager, inputs, x, y, w, h, initialState, openCloseTime) {
  val xpad = 24
  val ypad = 24
  
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

class PrintingTextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  timePerChar: Double,
  initialState: Int = Window.Opening,
  openCloseTime: Double = 0.25,
  linesPerBlock: Int = 4,
  justification: Int = Window.Left)
  extends Window(manager, inputs, x, y, w, h, initialState, openCloseTime) {
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
        changeState(Window.Closing)
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
