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
import rpgboss.player.MyGame

class ChoiceWindow(
  id: Long,
  game: MyGame,
  assets: RpgAssetManager,
  proj: Project,
  choices: Array[String] = Array(),
  x: Int, y: Int, w: Int, h: Int,
  skin: Windowskin,
  skinRegion: TextureRegion,
  fontbmp: BitmapFont,
  initialState: Int = Window.Opening,
  openCloseMs: Int = 250,
  justification: Int = Window.Left,
  defaultChoice: Int = 0,
  // Choices displayed in a row-major way.
  columns: Int = 1,
  // 0 shows all the lines. Positive numbers for scrolling.
  displayedLines: Int = 0,
  allowCancel: Boolean)
  extends Window(
      id, game, assets, proj, x, y, w, h, skin, skinRegion, fontbmp,
      initialState, openCloseMs)
  with ChoiceInputHandler {
  val xpad = 24
  val ypad = 24
  val textWTotal = w - 2*xpad
  val textHTotal = h - 2*ypad
  val textColW = textWTotal / columns
  
  private var curChoice = defaultChoice
  
  def wrapChoices = displayedLines == 0
  
  override val capturedKeys = 
    Set(MyKeys.Left, MyKeys.Right, MyKeys.Up, MyKeys.Down, 
        MyKeys.OK, MyKeys.Cancel)
        
  var scrollXPosition = 0
  var textImages: Array[WindowText] = {
    val columnChoicesAry = 
      Array.fill(columns)(new collection.mutable.ArrayBuffer[String]())
    for (i <- 0 until choices.length) {
      columnChoicesAry(i % columns).append(choices(i))
    }
    
    val windowTexts = for (i <- 0 until columns) yield new WindowText(
      game,
      columnChoicesAry(i).toArray,
      x + xpad + textColW*i,
      y + ypad,
      textColW,
      textHTotal,
      fontbmp,
      justification
    )
    
    windowTexts.toArray
  }
  
  override def update(delta: Float, acceptInput: Boolean) = {
    super.update(delta, acceptInput)
    textImages.foreach(_.update())
  }
        
  val choiceChannel = new Channel[Int]()
  
  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(proj, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }
  
  val soundSelect = optionallyReadAndLoad(proj.data.startup.soundSelect)
  val soundCursor = optionallyReadAndLoad(proj.data.startup.soundCursor)
  val soundCancel = optionallyReadAndLoad(proj.data.startup.soundCancel)
  val soundCannot = optionallyReadAndLoad(proj.data.startup.soundCannot)
  
  def keyActivate(key: Int) = {
    import MyKeys._
    
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    import MyKeys._
    if (key == Up) {
      curChoice -= columns
      if (curChoice < 0) {
        if (wrapChoices) {
          curChoice += choices.length
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
      if (curChoice >= choices.length) {
        if (wrapChoices) {
          curChoice -= choices.length
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
      println("key OK")
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }
    
    if (key == Cancel && allowCancel) {
      println("key Cancel")
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }
  }

  def hasFocus = game.inputs.hasFocus(this)
  
  def takeFocus() = game.syncRun {
    println("takeFocus %d".format(id))
      
    game.screenLayer.windows.find(_.id == id).map { window =>
      game.inputs.remove(window)
      game.screenLayer.windows -= window
      game.inputs.prepend(window)
      game.screenLayer.windows.prepend(window)
    }
  }
  
  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)
    
    if (state == Window.Open || state == Window.Opening) {
      val renderedLines = 
        if (displayedLines == 0) choices.length else displayedLines
      textImages.foreach(_.render(b, scrollXPosition, renderedLines))

      // Now draw the cursor if not completed
      if (state == Window.Open && hasFocus) {
        val cursorX =
          x + xpad + (curChoice % columns)*textColW - 32
        val cursorY =
          y + ypad + (curChoice / columns)*textImages(0).lineHeight - 8
        skin.drawCursor(b, skinRegion, cursorX, cursorY, 32f, 32f)
      }
    }
  }

  // This method is safe to call on multiple threads
  def getChoice() = choiceChannel.read
}