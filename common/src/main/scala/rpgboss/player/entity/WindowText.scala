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

  def render(b: SpriteBatch, startLine: Int, linesToDraw: Int): Unit = {
    // Draw all complete lines in current block
    val endLine = math.min(text.length, startLine + linesToDraw)
    for (lineI <- startLine until endLine) {
      val offset = lineI - startLine
      drawText(b, text(lineI), 0, offset * lineHeight)
    }
  }

  def render(b: SpriteBatch): Unit = {
    render(b, 0, text.length)
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

  override def render(b: SpriteBatch): Unit = {
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
