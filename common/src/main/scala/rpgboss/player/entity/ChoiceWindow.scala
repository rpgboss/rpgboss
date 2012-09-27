package rpgboss.player.entity

import rpgboss.model.resource.Windowskin
import rpgboss.model.Project
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import akka.dispatch.Promise
import rpgboss.player.Global._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.player.MenuInputHandler
import rpgboss.player.MyKeys

class ChoiceWindow(
    proj: Project,
    choices: Array[String] = Array(),
    x: Int, y: Int, w: Int, h: Int,
    skin: Windowskin,
    skinRegion: TextureRegion,
    fontbmp: BitmapFont,
    state: Int = Window.Opening, 
    stateAge: Int = 0,
    openCloseFrames: Int = 25,
    framesPerChar: Int = 5,
    linesPerBlock: Int = 4,
    justification: Int = Window.Left,
    defaultChoice: Int = 0,
    allowCancel: Boolean = false)
  extends Window(
      proj, text = choices, x, y, w, h, skin, skinRegion, fontbmp,
      state, stateAge, openCloseFrames, framesPerChar, linesPerBlock,
      justification)
  with MenuInputHandler {
  
  var curChoice = defaultChoice
  
  def keyDelay = 0.6f
  def keyInterval = 0.15f
  
  def keyActivate(key: Int) = {
    import MyKeys._
    if(key == Up) {
      curChoice = 
        if(curChoice == 0)
          choices.length-1
        else
          curChoice-1
    } else if(key == Down) {
      curChoice += 1
      if(curChoice == choices.length)
        curChoice = 0
    }
  }
  
  override def render(b: SpriteBatch) = {
    // Draw the window and text
    super.render(b)
    
    // Now draw the cursor
    
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
  
  val result = Promise[Int]()
}
