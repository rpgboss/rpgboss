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

class ChoiceWindow(
  id: Long,
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
  closeOnSelect: Boolean,
  allowCancel: Boolean)
  extends TextWindow(
      id, assets, proj, choices, x, y, w, h, skin, skinRegion, fontbmp,
      initialState, openCloseMs, justification = justification)
  with ChoiceInputHandler {
  
  private var curChoice = defaultChoice
  
  override val capturedKeys = 
    Set(MyKeys.Left, MyKeys.Right, MyKeys.Up, MyKeys.Down, 
        MyKeys.OK, MyKeys.Cancel)
  
  val choiceChannel = new Channel[Int]()
  
  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(proj, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }
  
  val soundSelect = optionallyReadAndLoad(proj.data.startup.soundSelect)
  val soundCursor = optionallyReadAndLoad(proj.data.startup.soundCursor)
  val soundCancel = optionallyReadAndLoad(proj.data.startup.soundCancel)
  
  def keyActivate(key: Int) = {
    import MyKeys._
    
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    import MyKeys._
    if (key == Up) {
      curChoice =
        if (curChoice == 0)
          choices.length - 1
        else
          curChoice - 1
      soundCursor.map(_.getAsset(assets).play())
    } else if (key == Down) {
      curChoice += 1
      if (curChoice == choices.length)
        curChoice = 0
      soundCursor.map(_.getAsset(assets).play())
    }

    if (key == OK) {
      if (closeOnSelect)
        changeState(Window.Closing)
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }
    
    if (key == Cancel && allowCancel) {
      curChoice = -1
      changeState(Window.Closing)
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }
  }

  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)

    // Now draw the cursor if not completed
    if (state == Window.Open) {
      val textStartX =
        x + textImage.xpad

      skin.drawCursor(b, skinRegion, textStartX - 32,
        y + textImage.ypad + textImage.lineHeight * curChoice - 8, 32f, 32f)
    }
  }

  // This method is safe to call on multiple threads
  def getChoice() = choiceChannel.read
}