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

class WindowText(
  persistent: PersistentState,
  text: Array[String],
  x: Int, y: Int, w: Int, h: Int,
  fontbmp: BitmapFont,
  justification: Int = Window.Left,
  var lineHeight: Int = 32) {

  def setLineHeight(height: Int) = {
    lineHeight = height
  }

  def drawText(
    b: SpriteBatch, text: String,
    xOffset: Float, yOffset: Float) = {
    val namesProcessedText = Window.nameReplace(
        text,
        persistent.getStringArray(ScriptInterfaceConstants.CHARACTER_NAMES))

    val intProcessedText =
      Window.variableReplace(namesProcessedText, persistent)

    val processedText = intProcessedText

    val colorCodesExist = Window.colorCtrl.findFirstIn(processedText).isDefined

    // Make left-aligned if color codes exist
    val fontAlign = if (colorCodesExist) {
      HAlignment.LEFT
    } else {
      justification match {
        case Window.Left => HAlignment.LEFT
        case Window.Center => HAlignment.CENTER
        case Window.Right => HAlignment.RIGHT
      }
    }

    // Draw shadow
    fontbmp.setColor(Color.BLACK)
    fontbmp.drawMultiLine(b, processedText,
      x + xOffset + 2,
      y + yOffset + 2,
      w, fontAlign)

    fontbmp.setColor(Color.WHITE)

    // Finds first color token, prints everything behind it, sets the color
    // and then does it again with the remaining text.
    @annotation.tailrec
    def printUntilColorTokenOrEnd(remainingText: CharSequence,
                                  xStart: Float): Unit = {
      if (remainingText.length() == 0)
        return

      val rMatchOption = Window.colorCtrl.findFirstMatchIn(remainingText)
      val textToPrintNow = rMatchOption.map(_.before).getOrElse(remainingText)

      val textBounds =
        fontbmp.drawMultiLine(b, textToPrintNow,
          xStart,
          y + yOffset,
          w, fontAlign)

      printUntilColorTokenOrEnd(rMatchOption.map(_.after).getOrElse(""),
                                xStart + textBounds.width)
    }

    printUntilColorTokenOrEnd(processedText, x + xOffset)
  }

  def update() = {}

  def render(b: SpriteBatch, startLine: Int, linesToDraw: Int) = {
    // Draw all complete lines in current block
    val endLine = math.min(text.length, startLine + linesToDraw)
    for (lineI <- startLine until endLine) {
      val offset = lineI - startLine
      drawText(b, text(lineI), 0, offset * lineHeight)
    }
  }
}

class PrintingWindowText(
  persistent: PersistentState,
  text: Array[String],
  x: Int, y: Int, w: Int, h: Int,
  skin: Windowskin,
  skinRegion: TextureRegion,
  fontbmp: BitmapFont,
  msPerChar: Int = 50,
  linesPerBlock: Int = 4,
  justification: Int = Window.Left)
  extends WindowText(persistent, text, x, y, w, h, fontbmp, justification) {

  def drawAwaitingArrow = true

  // If display instantly...
  var lineI = if (msPerChar == 0) text.length else 0
  var charI = 0
  var blockI = 0

  // Get age of text image in milliseconds
  var lastCharPrintTime = System.currentTimeMillis()
  def timeSinceLastChar = System.currentTimeMillis() - lastCharPrintTime

  def lineInCurrentBlock = lineI < (blockI + 1) * linesPerBlock
  def allTextPrinted = !(lineI < text.length)
  def awaitingInput = allTextPrinted || !lineInCurrentBlock

  override def update() = {
    // Do this update if:
    //   - Text is shown gradually
    //   - We haven't exhausted all the text
    //   - The lineI is in the current block
    if (msPerChar > 0 && !allTextPrinted && lineInCurrentBlock) {
      while (timeSinceLastChar > msPerChar) {
        val line = text(lineI)

        charI += 1

        // advance line if:
        //  - Out of characters
        //  - It's not the last line of the block
        if (charI >= line.length()) {
          lineI += 1
          charI = 0
        }

        lastCharPrintTime = System.currentTimeMillis()
      }
    }
  }

  def render(b: SpriteBatch) = {
    // Draw all complete lines in current block
    for (i <- blockI * linesPerBlock to (lineI - 1)) {
      val idxInBlock = i % linesPerBlock
      drawText(b, text(i), 0, idxInBlock * lineHeight)
    }

    // Draw the currently writing line
    if (lineI < text.length) {
      val idxInBlock = lineI % linesPerBlock
      drawText(b, text(lineI).take(charI), 0, idxInBlock * lineHeight)
    }

    // If waiting for user input to finish, draw the arrow
    if (drawAwaitingArrow && awaitingInput) {
      skin.drawArrow(b, skinRegion,
        x + w / 2 - 8, y + h, 16, 16)
    }
  }
}
