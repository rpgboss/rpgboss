package rpgboss.player
import rpgboss.player.entity._

/**
 * This class provides the interface of commands that Javascript can call
 */
class ScriptInterface(game: MyGame) {
  def state = game.state
  
  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int, h: Int) =
    state.showPicture(slot, name, x, y, w, h)
  
  def hidePicture(slot: Int) = 
    state.hidePicture(slot)
  
  def choiceWindow(
      choices: Array[String],
      x: Int, y: Int, w: Int, h: Int,
      justification: Int) = {
    val winW = 200
    state.windows.push(new ChoiceWindow(game.project,
       choices,
       x, y, w, h,
       game.screenLayer.windowskin, 
       game.screenLayer.windowskinRegion, 
       game.screenLayer.fontbmp,
       state = Window.Opening,
       framesPerChar = 0,
       justification = justification)
    )
  }
  
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right
}