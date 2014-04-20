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

  val colorCtrl = """\\[Cc]\[(\d+)\]""".r
  val nameCtrl = """\\[Nn]\[(\d+)\]""".r
  val variableCtrl = """\\[Vv]\[([a-zA-Z_$][\w_$]*)\]""".r

  def nameReplace(raw: String, nameList: Array[String]) =
    nameCtrl.replaceAllIn(raw, rMatch => nameList(rMatch.group(1).toInt))
  def variableReplace(raw: String, persistent: PersistentState) = {
    variableCtrl.replaceAllIn(raw, rMatch =>
      persistent.getInt(rMatch.group(1)).toString)
  }
}

// stateAge starts at 0 and goes up as window opens or closes
class Window(
  val id: Long,
  screenLayer: WindowManager,
  inputs: InputMultiplexer,
  assets: RpgAssetManager,
  proj: Project,
  val x: Int, val y: Int, val w: Int, val h: Int,
  skin: Windowskin,
  skinRegion: TextureRegion,
  fontbmp: BitmapFont,
  initialState: Int = Window.Opening,
  openCloseMs: Int = 250)
  extends InputHandler {
  var state = initialState
  // Used to determine when to expire the state and move onto next state
  var stateStarttime = System.currentTimeMillis()
  def stateAge = System.currentTimeMillis() - stateStarttime

  def changeState(newState: Int) = {
    state = newState
    stateStarttime = System.currentTimeMillis()
  }

  def update(delta: Float, acceptInput: Boolean) = {
    // change state of "expired" opening or closing animations
    if (stateAge >= openCloseMs) {
      state match {
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

  def render(b: SpriteBatch) = state match {
    case Window.Open => {
      skin.draw(b, skinRegion, x, y, w, h)
    }
    case Window.Opening => {
      val hVisible =
        math.max(32 + (stateAge.toDouble / openCloseMs * (h - 32)).toInt, 32)

      skin.draw(b, skinRegion, x, y, w, hVisible)
    }
    case Window.Closing => {
      val hVisible =
        math.max(h - (stateAge.toDouble / openCloseMs * (h - 32)).toInt, 32)

      skin.draw(b, skinRegion, x, y, w, hVisible)
    }
    case _ => Unit
  }

  def close() = {
    if (state != Window.Closing && state != Window.Closed)
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
    screenLayer.windows -= this
  }

  // This is used to either convey a choice, or simply that the window
  // has been closed
  private val closePromise = Promise[Int]()
}

class TextWindow(
  id: Long,
  persistent: PersistentState,
  screenLayer: WindowManager,
  inputs: InputMultiplexer,
  assets: RpgAssetManager,
  proj: Project,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  skin: Windowskin,
  skinRegion: TextureRegion,
  fontbmp: BitmapFont,
  initialState: Int = Window.Opening,
  openCloseMs: Int = 250,
  justification: Int = Window.Left)
  extends Window(
    id, screenLayer, inputs, assets, proj, x, y, w, h, skin, skinRegion,
    fontbmp, initialState, openCloseMs) {
  val xpad = 24
  val ypad = 24

  val textImage = new WindowText(
    persistent,
    text,
    x + xpad,
    y + ypad,
    w - 2*xpad,
    h - 2*ypad,
    fontbmp,
    justification)

  override def update(delta: Float, acceptInput: Boolean) = {
    super.update(delta, acceptInput)
    textImage.update()
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
  id: Long,
  persistent: PersistentState,
  screenLayer: WindowManager,
  inputs: InputMultiplexer,
  assets: RpgAssetManager,
  proj: Project,
  text: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  skin: Windowskin,
  skinRegion: TextureRegion,
  fontbmp: BitmapFont,
  msPerChar: Int,
  initialState: Int = Window.Opening,
  openCloseMs: Int = 250,
  linesPerBlock: Int = 4,
  justification: Int = Window.Left)
  extends Window(
    id, screenLayer, inputs, assets, proj, x, y, w, h, skin, skinRegion,
    fontbmp, initialState, openCloseMs) {
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
    fontbmp,
    msPerChar,
    linesPerBlock,
    justification)

  override def keyDown(key: Int) = {
    import MyKeys._
    if (key == OK) {
      // If we have already printed the last line, set to closing
      // otherwise, advance the block.

      if (textImage.lineI < text.length) {
        textImage.lineI += 1
      } else {
        changeState(Window.Closing)
      }
    }
  }

  override def update(delta: Float, acceptInput: Boolean) = {
    super.update(delta, acceptInput)
    textImage.update()
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
