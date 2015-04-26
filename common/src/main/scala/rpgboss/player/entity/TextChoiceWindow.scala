package rpgboss.player.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.lib.GdxUtils.syncRun
import rpgboss.lib.Rect
import rpgboss.player.InputMultiplexer
import rpgboss.player.MyKeys.Cancel
import rpgboss.player.MyKeys.Down
import rpgboss.player.MyKeys.Left
import rpgboss.player.MyKeys.OK
import rpgboss.player.MyKeys.Right
import rpgboss.player.MyKeys.Up
import rpgboss.player.PersistentState
import rpgboss.player.WindowManager
import org.mozilla.javascript.NativeObject
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import java.io.File
import rpgboss.lib.FileHelper
import rpgboss.lib.JsonUtils
import rpgboss.model.Item
import rpgboss.lib.Utils
import rpgboss.lib.Layout

object TextChoiceWindow {
  val xpad = 24
  val ypad = 24
}

class TextChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  var lines: Array[String],
  layout: Layout,
  options: TextChoiceWindowOptions)
  extends ChoiceWindow(persistent, manager, inputs, layout,
                       invisible = false,
                       options.defaultChoice, options.allowCancel) {
  import TextChoiceWindow._
  assert(options.linesPerChoice != 0)

  def columns = options.columns
  assert(columns >= 1)
  def displayedLines = options.displayedLines
  def linesPerChoice = options.linesPerChoice

  override val rect =
    getRectFromLines(lines, options.displayedLines, xpad, ypad, columns)
  private var textWTotal = 0f
  private var textHTotal = 0f
  private var textColW = 0f

  val nChoices = lines.length / linesPerChoice

  val choiceRows = Utils.ceilIntDiv(nChoices, columns)

  def wrapChoices = options.displayedLines == 0

  var scrollTopLine = 0
  var textImages: Array[WindowText] = null

  private var _leftMargin = options.leftMargin

  private def updateTextImages() = {
    textWTotal = rect.w - 2*xpad - _leftMargin
    textHTotal = rect.h - 2*ypad
    textColW = textWTotal / columns

    val columnChoicesAry =
      Array.fill(columns)(new collection.mutable.ArrayBuffer[String]())
    for (i <- 0 until lines.length) {
      columnChoicesAry((i / linesPerChoice) % columns).append(lines(i))
    }

    val windowTexts = for (i <- 0 until columns) yield {
      val newRectX = rect.left + xpad + textColW*i + textColW / 2 + _leftMargin
      val windowTextRect = Rect(newRectX, rect.y, textColW, textHTotal)
      new WindowText(
        persistent,
        columnChoicesAry(i).toArray,
        windowTextRect,
        manager.fontbmp,
        options.justification,
        options.lineHeight
      )
    }

    textImages = windowTexts.toArray
  }

  updateTextImages()

  def updateLines(newLines: Array[String]) = {
    lines = newLines
    updateTextImages()

    if (lines.length == 0) {
      setCurChoice(0)
    } else {
      setCurChoice(math.min(curChoice, lines.length / linesPerChoice - 1))
    }

    updateScrollPosition()
  }

  def updateScrollPosition() = {
    if (displayedLines != 0) {
      scrollTopLine =
        ((curChoice * linesPerChoice) / displayedLines) * displayedLines
    }
  }

  override def update(delta: Float) = {
    super.update(delta)
    textImages.foreach(_.update(delta))
  }

  def keyActivate(key: Int): Unit = {
    if (state != Window.Open)
      return

    // TODO: Remove hack
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    val origChoiceX = curChoice % columns
    val origChoiceY = curChoice / columns

    var choiceX = origChoiceX
    var choiceY = origChoiceY

    def columnsInThatRow(row: Int): Int = {
      if (columns == 1)
        1
      else if (row < choiceRows - 1)
        columns
      else
        nChoices % columns
    }

    if (nChoices > 0 &&
        (key == Up || key == Down || key == Right || key == Left)) {
      if (key == Up && (choiceY > 0 || wrapChoices)) {
        choiceY -= 1
        choiceY = Utils.pmod(choiceY, choiceRows)
        choiceX = Utils.clamped(choiceX, 0, columnsInThatRow(choiceY) - 1)
      } else if (key == Down && (choiceY < choiceRows - 1 || wrapChoices)) {
        choiceY += 1
        choiceY = Utils.pmod(choiceY, choiceRows)
        choiceX = Utils.clamped(choiceX, 0, columnsInThatRow(choiceY) - 1)
      } else if (columns > 1) {
        if (key == Right && (choiceX < columns - 1 || wrapChoices)) {
          choiceX += 1
          choiceX = Utils.pmod(choiceX, columnsInThatRow(choiceY))
        } else if (key == Left && (choiceX > 0 || wrapChoices)) {
          choiceX -= 1
          choiceX = Utils.pmod(choiceX, columnsInThatRow(choiceY))
        }
      }

      if (choiceX != origChoiceX || choiceY != origChoiceY) {
        setCurChoice(choiceY * columns + choiceX)
        soundCursor.map(_.getAsset(assets).play())
        updateScrollPosition()
      } else {
        soundCannot.map(_.getAsset(assets).play())
      }
    }

    if (key == OK) {
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }

    if (key == Cancel && options.allowCancel) {
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }

    updateScrollPosition()
  }

  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)

    if (state == Window.Open || state == Window.Opening) {
      val renderedLines =
        if (displayedLines == 0) lines.length else displayedLines
      textImages.foreach(_.render(b, scrollTopLine, renderedLines))

      // Now draw the cursor if not completed
      if (state == Window.Open && inputs.hasFocus(this)) {
        val cursorLeft =
          rect.left + xpad + (curChoice % columns)*textColW - 32

        val yRowOffset =
          ((curChoice * linesPerChoice) / columns) - (scrollTopLine)
        val cursorTop =
          rect.top + ypad + yRowOffset * options.lineHeight - 8
        skin.drawCursor(b, skinTexture, cursorLeft, cursorTop)
      }

      if (displayedLines != 0) {
        val sections = Utils.ceilIntDiv(lines.length, renderedLines)
        val currentSection = scrollTopLine / displayedLines

        val totalScrollbarLength = rect.h
        val sectionLength = totalScrollbarLength / sections

        skin.draw(
          b, skinTexture,
          rect.right - 16, rect.top + currentSection * sectionLength,
          2, sectionLength,
          bordersOnly = true)
      }
    }
  }

  class TextChoiceWindowScriptInterface extends ChoiceWindowScriptInterface {
    def updateLines(lines: Array[String]) = syncRun {
      TextChoiceWindow.this.updateLines(lines)
    }
  }

  override lazy val scriptInterface = new TextChoiceWindowScriptInterface
}

case class TextChoiceWindowOptions(
  justification: Int = Window.Left,
  defaultChoice: Int = 0,
  // Choices displayed in a row-major way.
  columns: Int = 1,
  // 0 shows all the lines. Positive numbers for scrolling.
  displayedLines: Int = 0,
  // 1 means each choice occupies one line.
  linesPerChoice: Int = 1,
  allowCancel: Boolean = true,
  lineHeight: Int = 32,
  leftMargin: Float = 0)