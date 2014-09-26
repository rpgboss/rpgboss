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

class TextChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  var lines: Array[String],
  rect: Rect,
  options: TextChoiceWindowOptions)
  extends ChoiceWindow(persistent, manager, inputs, rect,
                       invisible = false,
                       options.defaultChoice, options.allowCancel) {
  def columns = options.columns
  def displayedLines = options.displayedLines
  def linesPerChoice = options.linesPerChoice

  val xpad = 24
  val ypad = 24
  val textWTotal = rect.w - 2*xpad
  val textHTotal = rect.h - 2*ypad
  val textColW = textWTotal / columns

  def nChoices = lines.length / linesPerChoice

  def wrapChoices = options.displayedLines == 0

  var scrollTopLine = 0
  var textImages: Array[WindowText] = null

  private def updateTextImages() = {
    val columnChoicesAry =
      Array.fill(columns)(new collection.mutable.ArrayBuffer[String]())
    for (i <- 0 until lines.length) {
      columnChoicesAry((i / linesPerChoice) % columns).append(lines(i))
    }

    val windowTexts = for (i <- 0 until columns) yield {
      val newRectX = rect.left + xpad + textColW*i + textColW / 2
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
    curChoice = math.min(curChoice, lines.length / linesPerChoice)
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

    if (key == Up) {
      curChoice -= columns
      if (curChoice < 0) {
        if (wrapChoices) {
          curChoice += nChoices
          soundCursor.map(_.getAsset(assets).play())
        } else {
          // Undo the subtraction we just did.
          curChoice += columns
          soundCannot.map(_.getAsset(assets).play())
        }
      } else {
        soundCursor.map(_.getAsset(assets).play())
      }
    } else if (key == Down) {
      curChoice += columns
      if (curChoice >= nChoices) {
        if (wrapChoices) {
          curChoice -= nChoices
          soundCursor.map(_.getAsset(assets).play())
        } else {
          // Undo the addition we just did
          curChoice -= columns
          soundCannot.map(_.getAsset(assets).play())
        }
      } else {
        soundCursor.map(_.getAsset(assets).play())
      }
    } else if (columns > 1) {
      if (key == Right) {
        // Go back to left if all the way on right
        if (curChoice % columns == columns - 1)
          curChoice -= (columns - 1)
        else
          curChoice += 1

        soundCursor.map(_.getAsset(assets).play())
      } else if (key == Left) {
        // Go back to right if all the way on left
        if (curChoice % columns == 0)
          curChoice += (columns - 1)
        else
          curChoice -= 1

        soundCursor.map(_.getAsset(assets).play())
      }
    }

    updateScrollPosition()

    if (key == OK) {
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }

    if (key == Cancel && options.allowCancel) {
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }
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
        skin.drawCursor(b, skinRegion, cursorLeft, cursorTop)
      }

      if (displayedLines != 0) {
        val sections = lines.length / renderedLines
        val currentSection = curChoice / displayedLines

        val totalScrollbarLength = rect.h
        val sectionLength = totalScrollbarLength / sections

        skin.draw(
          b, skinRegion,
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
  lineHeight: Int = 32)