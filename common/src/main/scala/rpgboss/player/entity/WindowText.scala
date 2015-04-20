package rpgboss.player.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.lib.Rect
import rpgboss.lib.ThreadChecked
import rpgboss.model.resource.Windowskin
import rpgboss.player.PersistentState
import rpgboss.player.ScriptInterfaceConstants
import scala.collection.mutable.ArrayBuffer

object WindowText {
  val colorCtrl = """\\[Cc]\[(\d+)\]""".r
  val nameCtrl = """\\[Nn]\[(\d+)\]""".r
  val variableCtrl = """\\[Vv]\[([a-zA-Z_$][\w_$]*)\]""".r
  val javascriptCtrl = """\\[Jj]\[([a-zA-Z_$][\w_$()]*)\]""".r

  // Provide 12 default colors. TODO: Make this customizable
  val colorSet = Array(
    Color.WHITE,
    Color.GRAY,
    Color.DARK_GRAY,
    Color.BLACK,
    Color.RED,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN,
    Color.BLUE,
    Color.PURPLE,
    Color.MAGENTA,
    Color.PINK)

  val NAME_OUT_OF_BOUNDS = "NAME_OUT_OF_BOUNDS"

  def nameReplace(raw: String, nameList: Array[String]) = {
    nameCtrl.replaceAllIn(raw, rMatch => {
      val index = rMatch.group(1).toInt
      if (index < nameList.length) nameList(index) else NAME_OUT_OF_BOUNDS
    })
  }

  def variableReplace(raw: String, persistent: PersistentState) = {
    variableCtrl.replaceAllIn(raw, rMatch => {
      persistent.getInt(rMatch.group(1)).toString
    })
  }

  def processText(text: Array[String], persistent: PersistentState) = {
    val newText = for (line <- text) yield {
      val namesProcessedLine = WindowText.nameReplace(
        line,
        persistent.getStringArray(ScriptInterfaceConstants.CHARACTER_NAMES))

      WindowText.variableReplace(namesProcessedLine, persistent)
    }

    newText.toArray
  }

  def DefaultLineHeight = 32
}

class WindowText(
  persistent: PersistentState,
  initialText: Array[String],
  private var rect: Rect,
  fontbmp: BitmapFont,
  justification: Int = Window.Left,
  val lineHeight: Int = WindowText.DefaultLineHeight)
  extends ThreadChecked with LazyLogging {

  def processText(text: Array[String]) =
    WindowText.processText(text, persistent)

  protected var _text: Array[String] = processText(initialText)

  def updateRect(rect: Rect) = {
    this.rect = rect
  }

  def updateText(newText: Array[String]) = _text = processText(newText)

  def drawLine(b: SpriteBatch, line: String, xOffset: Float, yOffset: Float) = {
    val colorCodesExist = WindowText.colorCtrl.findFirstIn(line).isDefined

    // Force left-aligned if color codes exist, as we can't draw the
    val fontAlign = if (colorCodesExist) {
      HAlignment.LEFT
    } else {
      justification match {
        case Window.Left   => HAlignment.LEFT
        case Window.Center => HAlignment.CENTER
        case Window.Right  => HAlignment.RIGHT
      }
    }

    // Finds first color token, prints everything behind it, sets the color
    // and then does it again with the remaining text.
    @annotation.tailrec
    def printUntilColorTokenOrEnd(remainingText: CharSequence,
                                  xStart: Float): Unit = {
      if (remainingText.length() == 0)
        return

      val rMatchOption = WindowText.colorCtrl.findFirstMatchIn(remainingText)
      val textToPrintNow = rMatchOption.map(_.before).getOrElse(remainingText)


      // Draw Shadow
      val textBounds = fontbmp.drawMultiLine(b, textToPrintNow, xStart,
        rect.top + yOffset,
        rect.w, fontAlign)

      // Set color to the desired one...
      rMatchOption.map { rMatch =>
        val groupOne = rMatch.group(1)
        try {
          val colorId = groupOne.toInt
          if (colorId < WindowText.colorSet.length) {
            fontbmp.setColor(WindowText.colorSet(colorId))
          } else {
            logger.warn("Color code out of bounds: %s".format(colorId))
            fontbmp.setColor(Color.WHITE)
          }
        } catch {
          case e: Exception =>
            logger.warn("Could not interpret color code: %s".format(
              rMatch.matched))
            fontbmp.setColor(Color.WHITE)
        }
      }

      printUntilColorTokenOrEnd(rMatchOption.map(_.after).getOrElse(""),
        xStart + textBounds.width)
    }

    printUntilColorTokenOrEnd(line, rect.left + xOffset)

    fontbmp.setColor(Color.WHITE)
  }

  def update(delta: Float) = {}

  def render(b: SpriteBatch, startLine: Int, linesToDraw: Int): Unit = {
    val endLine = math.min(_text.length, startLine + linesToDraw)
    for (lineI <- startLine until endLine) {
      val offset = lineI - startLine
      drawLine(b, _text(lineI), 0, offset * lineHeight)
    }
  }

  def render(b: SpriteBatch): Unit = {
    render(b, 0, _text.length)
  }
}

object PrintingWindowText {
  def wrapLine(line: String, maxWidth: Float,
               widthFunction: String => Float) = {
    val wrappedLines = new ArrayBuffer[String]

    val words = line.split(" ")
    var lineStart = 0
    // Minimum one word per line.
    var wordsInLine = 1
    while (lineStart < words.length) {
      // Candidate line is the line with one more word.
      val lineWithOneMoreWord =
        words.view(lineStart, lineStart + wordsInLine + 1).mkString(" ")

      if (lineStart + wordsInLine < words.length &&
          widthFunction(lineWithOneMoreWord) <= maxWidth) {
        // We can fit one more word in, so try fitting another one.
        wordsInLine += 1;
      } else {
        // Break off a line.
        wrappedLines +=
          words.view(lineStart, lineStart + wordsInLine).mkString(" ")
        lineStart += wordsInLine
        wordsInLine = 1
      }
    }

    wrappedLines.toArray
  }
}

class PrintingWindowText(
  persistent: PersistentState,
  initialText: Array[String],
  rect: Rect,
  skin: Windowskin,
  skinTexture: Texture,
  fontbmp: BitmapFont,
  options: PrintingTextWindowOptions)
  extends WindowText(
    persistent, initialText, rect, fontbmp, options.justification) {
  assume(options.timePerChar >= 0)

  override def processText(text: Array[String]): Array[String] = {
    if (fontbmp == null)
      return text

    val lineHeight = fontbmp.getLineHeight()
    val processedText = super.processText(text)

    val wrappedLines = new ArrayBuffer[String]

    for (line <- processedText) {
      wrappedLines ++= PrintingWindowText.wrapLine(
          line, rect.w, fontbmp.getBounds(_).width)
    }

    wrappedLines.toArray
  }

  def drawAwaitingArrow = true

  /**
   * When this is in the next block, the whole block has been printed.
   */
  private var _lineI = {
    if (options.timePerChar == 0)
      math.min(_text.length, options.linesPerBlock)
    else
      0
  }

  private var _charI = 0
  private var _blockI = 0

  /**
   * Game time since the last character was printed. This is usually left to be
   * non-zero after each frame, since we conceptually print characters between
   * frames.
   */
  private var _timeSinceLastCharacter: Float = 0

  def wholeBlockPrinted = _lineI >= (_blockI + 1) * options.linesPerBlock
  def allTextPrinted = !(_lineI < _text.length)

  def awaitingInput = allTextPrinted || wholeBlockPrinted

  def advanceBlock() = {
    _blockI += 1
    _timeSinceLastCharacter = 0

    if (options.timePerChar == 0) {
      _lineI = math.min(_text.length, (_blockI + 1) * options.linesPerBlock)
    } else {
      assert(_lineI == _blockI * options.linesPerBlock)
      assert(_charI == 0)
    }
  }

  /**
   * Used to print some text faster if the user is getting impatient.
   */
  def speedThrough() =
    _timeSinceLastCharacter += 1.0f

  override def updateText(newText: Array[String]) = {
    super.updateText(newText)

    _lineI = if (options.timePerChar == 0)
      math.min(_text.length, options.linesPerBlock)
    else
      0

    _charI = 0
    _blockI = 0
    _timeSinceLastCharacter = 0
  }

  override def update(delta: Float): Unit = {
    if (awaitingInput)
      return

    _timeSinceLastCharacter += delta

    // This loop advances at most one line per iteration.
    while (!wholeBlockPrinted && !allTextPrinted &&
      _timeSinceLastCharacter > options.timePerChar) {
      assert(_lineI <= _text.length)
      val line = _text(_lineI)

      val charsLeftInLine = line.length() - _charI
      val charsWeHaveTimeToPrint =
        (_timeSinceLastCharacter / options.timePerChar).toInt
      val charsAdvanced = math.min(charsLeftInLine, charsWeHaveTimeToPrint)

      _charI += charsAdvanced

      if (_charI >= line.length()) {
        _lineI += 1
        _charI = 0
      }

      _timeSinceLastCharacter -= charsAdvanced * options.timePerChar
    }
  }

  override def render(b: SpriteBatch): Unit = {
    // Draw all complete lines in current block
    for (i <- _blockI * options.linesPerBlock to (_lineI - 1)) {
      val idxInBlock = i % options.linesPerBlock
      drawLine(b, _text(i), 0, idxInBlock * lineHeight)
    }

    // Draw the currently writing line
    if (_lineI < _text.length) {
      val idxInBlock = _lineI % options.linesPerBlock
      drawLine(b, _text(_lineI).take(_charI), 0, idxInBlock * lineHeight)
    }

    // If waiting for user input to finish, draw the arrow
    if (options.showArrow && awaitingInput) {
      skin.drawArrow(b, skinTexture, rect.x - 8, rect.bot)
    }
  }
}
