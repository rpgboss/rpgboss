package rpgboss.player.entity

import rpgboss.model.resource._
import rpgboss.model.Project
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import akka.dispatch.Promise
import rpgboss.player.Global._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.player.ChoiceInputHandler
import rpgboss.player.MyKeys
import com.badlogic.gdx.assets.AssetManager

class ChoiceWindow(
    assets: RpgAssetManager,
    proj: Project,
    choices: Array[String] = Array(),
    x: Int, y: Int, w: Int, h: Int,
    skin: Windowskin,
    skinRegion: TextureRegion,
    fontbmp: BitmapFont,
    initialState: Int = Window.Opening, 
    openCloseMs: Int = 250,
    msPerChar: Int = 0,
    linesPerBlock: Int = 4,
    justification: Int = Window.Left,
    defaultChoice: Int = 0,
    allowCancel: Boolean = false)
  extends Window(
      assets, proj, text = choices, x, y, w, h, skin, skinRegion, fontbmp,
      state = initialState, openCloseMs, msPerChar, linesPerBlock,
      justification)
  with ChoiceInputHandler {
  
  var curChoice = defaultChoice
  
  val soundCursor = Sound.readFromDisk(proj, proj.data.soundCursor)
  val soundSelect = Sound.readFromDisk(proj, proj.data.soundSelect)
  val soundCancel = Sound.readFromDisk(proj, proj.data.soundCancel)
  val soundCannot = Sound.readFromDisk(proj, proj.data.soundCannot)
  soundCursor.loadAsset(assets)
  soundSelect.loadAsset(assets)
  soundCancel.loadAsset(assets)
  soundCannot.loadAsset(assets)
  
  def keyActivate(key: Int) = {
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()
    
    import MyKeys._
    if(key == Up) {
      curChoice = 
        if(curChoice == 0)
          choices.length-1
        else
          curChoice-1
      soundCursor.getAsset(assets).play()
    } else if(key == Down) {
      curChoice += 1
      if(curChoice == choices.length)
        curChoice = 0
      soundCursor.getAsset(assets).play()
    }
    
    if(key == A && !result.isCompleted) {
      changeState(Window.Closing)
      soundSelect.getAsset(assets).play()
    }
  }
  
  override def postClose() = {
    // Fulfill the promise and close after animation complete
    result.success(curChoice)
  }
  
  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)
    
    // Now draw the cursor if not completed
    if(state == Window.Open) {
      val textStartX = 
        x+textImage.xpad
      
      b.draw(
          skinRegion.getTexture(),
          textStartX - 32,
          y+textImage.ypad + textImage.lineHeight*curChoice - 8,
          32f, 32f,
          skinRegion.getRegionX()+2*32, 
          skinRegion.getRegionY()+3*32,
          32, 32,
          false, true
          )
    }
  }
  
  val result = Promise[Int]()
}
