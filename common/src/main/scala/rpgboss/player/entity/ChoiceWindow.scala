package rpgboss.player.entity

import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import scala.concurrent.Promise
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.player.ChoiceInputHandler
import rpgboss.player.MyKeys
import com.badlogic.gdx.assets.AssetManager
import scala.concurrent._
import scala.concurrent.duration.Duration
import rpgboss.player._
import rpgboss.lib.GdxUtils

abstract class ChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  rect: Rect,
  invisible: Boolean = false,
  defaultChoice: Int = 0,
  allowCancel: Boolean = true)
  extends Window(manager, inputs, rect, invisible)
  with ChoiceInputHandler {

  protected var curChoice = defaultChoice

  override def capturedKeys =
    Set(MyKeys.Left, MyKeys.Right, MyKeys.Up, MyKeys.Down,
        MyKeys.OK, MyKeys.Cancel)

  val choiceChannel = new Channel[Int]()

  def project = manager.project
  def assets = manager.assets

  override def startClosing() = {
    super.startClosing()
    choiceChannel.write(-1)
  }

  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(project, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }

  val soundSelect = optionallyReadAndLoad(project.data.startup.soundSelect)
  val soundCursor = optionallyReadAndLoad(project.data.startup.soundCursor)
  val soundCancel = optionallyReadAndLoad(project.data.startup.soundCancel)
  val soundCannot = optionallyReadAndLoad(project.data.startup.soundCannot)

  class ChoiceWindowScriptInterface extends WindowScriptInterface {
    import GdxUtils._

    def getChoice() = choiceChannel.read

    def takeFocus(): Unit = syncRun {
      inputs.remove(ChoiceWindow.this)
      inputs.prepend(ChoiceWindow.this)
      manager.focusWindow(ChoiceWindow.this)
    }
  }

  override lazy val scriptInterface = new ChoiceWindowScriptInterface
}

class TextChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  var lines: Array[String],
  rect: Rect,
  justification: Int = Window.Left,
  defaultChoice: Int = 0,
  // Choices displayed in a row-major way.
  columns: Int = 1,
  // 0 shows all the lines. Positive numbers for scrolling.
  displayedLines: Int = 0,
  allowCancel: Boolean = true)
  extends ChoiceWindow(persistent, manager, inputs, rect,
                       invisible = false,
                       defaultChoice, allowCancel) {
  val xpad = 24
  val ypad = 24
  val textWTotal = rect.w - 2*xpad
  val textHTotal = rect.h - 2*ypad
  val textColW = textWTotal / columns

  def wrapChoices = displayedLines == 0

  var scrollTopLine = 0
  var textImages: Array[WindowText] = null

  private def updateTextImages() = {
    val columnChoicesAry =
      Array.fill(columns)(new collection.mutable.ArrayBuffer[String]())
    for (i <- 0 until lines.length) {
      columnChoicesAry(i % columns).append(lines(i))
    }

    val windowTexts = for (i <- 0 until columns) yield {
      val newRectX = rect.left + xpad + textColW*i + textColW / 2
      val windowTextRect = Rect(newRectX, rect.y, textColW, textHTotal)
      new WindowText(
        persistent,
        columnChoicesAry(i).toArray,
        windowTextRect,
        manager.fontbmp,
        justification
      )
    }

    textImages = windowTexts.toArray
  }

  updateTextImages()

  def updateLines(newLines: Array[String]) = {
    lines = newLines
    updateTextImages()
    curChoice = math.max(curChoice, lines.length)
    updateScrollPosition()
  }

  def updateScrollPosition() = {
    if (displayedLines != 0) {
      scrollTopLine = (curChoice / displayedLines) * displayedLines
    }
  }

  override def update(delta: Float) = {
    super.update(delta)
    textImages.foreach(_.update(delta))
  }

  def keyActivate(key: Int): Unit = {
    import MyKeys._

    if (state != Window.Open)
      return

    // TODO: Remove hack
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    import MyKeys._
    if (key == Up) {
      curChoice -= columns
      if (curChoice < 0) {
        if (wrapChoices) {
          curChoice += lines.length
          soundCursor.map(_.getAsset(assets).play())
        } else {
          curChoice += columns
          soundCannot.map(_.getAsset(assets).play())
        }
      } else {
        soundCursor.map(_.getAsset(assets).play())
      }
    } else if (key == Down) {
      curChoice += columns
      if (curChoice >= lines.length) {
        if (wrapChoices) {
          curChoice -= lines.length
          soundCursor.map(_.getAsset(assets).play())
        } else {
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

    if (key == Cancel && allowCancel) {
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

        val yRowOffset = (curChoice / columns) - (scrollTopLine)
        val cursorTop =
          rect.top + ypad + yRowOffset * textImages(0).lineHeight - 8
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
    import GdxUtils._

    def setLineHeight(height: Int) = syncRun {
      textImages.foreach(_.setLineHeight(height))
    }
  }

  override lazy val scriptInterface = new TextChoiceWindowScriptInterface
}

/**
 * @param   choices     Is an Array[Set[Rect]] to support some choices being
 *                      defined by multiple rectangles on screen. For instance,
 *                      selecting all the members of your party during a battle.
 */
class SpatialChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  choices: Array[Set[Rect]] = Array(),
  defaultChoice: Int = 0)
  extends ChoiceWindow(persistent, manager, inputs, Rect(0, 0, 0, 0),
                       invisible = true, defaultChoice, allowCancel = true) {
  def keyActivate(key: Int): Unit = {
    import MyKeys._

    if (state != Window.Open)
      return

    // TODO: Remove hack
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    import MyKeys._
    if (key == Up || key == Left) {
      curChoice = Utils.pmod(curChoice - 1, choices.length)
      soundCursor.map(_.getAsset(assets).play())
    } else if (key == Down || key == Right) {
      curChoice = Utils.pmod(curChoice + 1, choices.length)
      soundCursor.map(_.getAsset(assets).play())
    }

    if (key == OK) {
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }

    if (key == Cancel) {
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }
  }

  override def render(b: SpriteBatch): Unit = {
    // Draw the window and text
    super.render(b)

    if (curChoice >= choices.length || curChoice < 0)
      return

    for (choiceRect <- choices(curChoice)) {
      skin.draw(b, skinRegion,
                choiceRect.left, choiceRect.top, choiceRect.w, choiceRect.h,
                bordersOnly = true)
    }
  }
}