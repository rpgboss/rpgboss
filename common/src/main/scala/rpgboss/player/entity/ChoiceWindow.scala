package rpgboss.player.entity

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

class ChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  lines: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  initialState: Int = Window.Opening,
  openCloseTime: Double = 0.25,
  justification: Int = Window.Left,
  defaultChoice: Int = 0,
  // Choices displayed in a row-major way.
  columns: Int = 1,
  // 0 shows all the lines. Positive numbers for scrolling.
  displayedLines: Int = 0,
  allowCancel: Boolean = true)
  extends Window(manager, inputs, x, y, w, h, initialState, openCloseTime)
  with ChoiceInputHandler {
  val xpad = 24
  val ypad = 24
  val textWTotal = w - 2*xpad
  val textHTotal = h - 2*ypad
  val textColW = textWTotal / columns

  private var curChoice = defaultChoice

  def wrapChoices = displayedLines == 0

  override def capturedKeys =
    Set(MyKeys.Left, MyKeys.Right, MyKeys.Up, MyKeys.Down,
        MyKeys.OK, MyKeys.Cancel)

  var scrollXPosition = 0
  val textImages: Array[WindowText] = {
    val columnChoicesAry =
      Array.fill(columns)(new collection.mutable.ArrayBuffer[String]())
    for (i <- 0 until lines.length) {
      columnChoicesAry(i % columns).append(lines(i))
    }

    val windowTexts = for (i <- 0 until columns) yield new WindowText(
      persistent,
      columnChoicesAry(i).toArray,
      x + xpad + textColW*i,
      y + ypad,
      textColW,
      textHTotal,
      manager.fontbmp,
      justification
    )

    windowTexts.toArray
  }

  override def update(delta: Float) = {
    super.update(delta)
    textImages.foreach(_.update(delta))
  }

  val choiceChannel = new Channel[Int]()

  def project = manager.project
  def assets = manager.assets
  
  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(project, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }

  val soundSelect = optionallyReadAndLoad(project.data.startup.soundSelect)
  val soundCursor = optionallyReadAndLoad(project.data.startup.soundCursor)
  val soundCancel = optionallyReadAndLoad(project.data.startup.soundCancel)
  val soundCannot = optionallyReadAndLoad(project.data.startup.soundCannot)

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
      textImages.foreach(_.render(b, scrollXPosition, renderedLines))

      // Now draw the cursor if not completed
      if (state == Window.Open && inputs.hasFocus(this)) {
        val cursorX =
          x + xpad + (curChoice % columns)*textColW - 32
        val cursorY =
          y + ypad + (curChoice / columns)*textImages(0).lineHeight - 8
        skin.drawCursor(b, skinRegion, cursorX, cursorY, 32f, 32f)
      }
    }
  }

  override lazy val scriptInterface = new WindowScriptInterface {
    import GdxUtils._
    
    def getChoice() = choiceChannel.read
    
    def takeFocus(): Unit = syncRun {
      inputs.remove(ChoiceWindow.this)
      inputs.prepend(ChoiceWindow.this)
      manager.focusWindow(ChoiceWindow.this)
    }
    
    def setLineHeight(height: Int) = syncRun {
      textImages.foreach(_.setLineHeight(height))
    }
  }
}