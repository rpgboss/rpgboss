package rpgboss.player.entity

import rpgboss.model.resource.Windowskin
import rpgboss.model.Project
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont

class ChoiceWindow(
    proj: Project,
    text: Array[String] = Array(),
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
    defaultChoice: Int = 0)
  extends Window(
      proj, text, x, y, w, h, skin, skinRegion, fontbmp,
      state, stateAge, openCloseFrames, framesPerChar, linesPerBlock,
      justification) {
  
  var choice = defaultChoice
  
  override def update(acceptInput: Boolean) = {
    super.update(acceptInput)
  }
}
