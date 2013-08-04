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

class ChoiceText(
  assets: RpgAssetManager,
  proj: Project,
  choices: Array[String],
  initialChoice: Int,
  x: Int, y: Int, w: Int, h: Int,
  fontbmp: BitmapFont,
  justification: Int = Window.Left) 
  extends WindowText(
    choices, x, y, w, h, fontbmp, justification)
  with ChoiceInputHandler {
  
  var curChoice = initialChoice
  
  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(proj, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }
  
  val soundCursor = optionallyReadAndLoad(proj.data.startup.soundCursor)
  
  def keyActivate(key: Int) = {
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
  }
}

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
  defaultChoice: Int = 0)
  extends TextWindow(
      id, assets, proj, choices, x, y, w, h, skin, skinRegion, fontbmp,
      initialState, openCloseMs)
  with ChoiceInputHandler {
  
  override val capturedKeys = Set(MyKeys.Up, MyKeys.Down, MyKeys.OK)
  
  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(proj, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }
  
  override val textImage = 
    new ChoiceText(assets, proj, choices, defaultChoice, x, y, w, h, fontbmp, 
        justification)
  
  val soundSelect = optionallyReadAndLoad(proj.data.startup.soundSelect)
  
  def keyActivate(key: Int) = {
    import MyKeys._
    textImage.keyActivate(key)

    if (key == OK && !result.isCompleted) {
      changeState(Window.Closing)
      soundSelect.map(_.getAsset(assets).play())
    }
  }

  override def postClose() = {
    // Fulfill the promise and close after animation complete
    result.success(textImage.curChoice)
  }

  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)

    // Now draw the cursor if not completed
    if (state == Window.Open) {
      val textStartX =
        x + textImage.xpad

      skin.drawCursor(b, skinRegion,
        textStartX - 32,
        y + textImage.ypad + textImage.lineHeight * textImage.curChoice - 8,
        32f, 32f)
    }
  }
}