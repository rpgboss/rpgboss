package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import java.awt._
import java.awt.image._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.assets.AssetManager
import akka.dispatch.Promise
import rpgboss.player.Global._
import rpgboss.player.InputHandler
import rpgboss.player.MyKeys

object Window {
  val Opening = 0
  val Open    = 1
  val Closing = 2
  
  val Left = 0
  val Center = 1
  val Right = 2
  
  val nameCtrl = """\\N\[(\d+)\]""".r
  var variableNameR = """[a-zA-Z_$][\w_$]*""".r
  val variableCtrl = """\\V\[([a-zA-Z_$][\w_$]*)\]""".r
  def nameReplace(raw: String, nameList: Array[String]) =
    nameCtrl.replaceAllIn(raw, rMatch => nameList(rMatch.group(1).toInt))
}

// stateAge starts at 0 and goes up as window opens or closes
case class Window(assets: RpgAssetManager,
                  proj: Project,
                  text: Array[String] = Array(),
                  x: Int, y: Int, w: Int, h: Int,
                  skin: Windowskin,
                  skinRegion: TextureRegion,
                  fontbmp: BitmapFont,
                  initialState: Int = Window.Opening,
                  openCloseMs: Int = 250,
                  msPerChar: Int = 80,
                  linesPerBlock: Int = 4,
                  justification: Int = Window.Left) 
  extends InputHandler
{
  var state = initialState
  // Used to determine when to expire the state and move onto next state
  var stateStarttime = System.currentTimeMillis()
  def stateAge = System.currentTimeMillis() - stateStarttime
  
  def drawAwaitingArrow = true
  
  val window = this
  
  object textImage {
    val xpad = 24f
    val ypad = 24f
    val textW = w-2*xpad
    val textH = h-2*ypad
    val lineHeight = 32f
    
    // If display instantly...
    var lineI = if(msPerChar == 0) text.length else 0
    var charI = 0
    var blockI = 0
    
    // Get age of text image in milliseconds
    var lastCharPrintTime = System.currentTimeMillis()
    def timeSinceLastChar = System.currentTimeMillis() - lastCharPrintTime
    
    val fontAlign = justification match {
      case Window.Left => HAlignment.LEFT
      case Window.Center => HAlignment.CENTER
      case Window.Right => HAlignment.RIGHT
    }
    
    def drawText(
        b: SpriteBatch, text: String, 
        x: Float, y: Float) : BitmapFont.TextBounds = {
      // Draw shadow
      fontbmp.setColor(Color.BLACK)
      fontbmp.drawMultiLine(b, text, 
          window.x + xpad + x + 2, 
          window.y + ypad + y + 2, 
          textW, fontAlign)
      
      fontbmp.setColor(Color.WHITE)
      fontbmp.drawMultiLine(b, text, 
          window.x + xpad + x, 
          window.y + ypad + y, 
          textW, fontAlign)
    }
    
    def lineInCurrentBlock = lineI < (blockI+1)*linesPerBlock
    def allTextPrinted = !(lineI < text.length)
    def awaitingInput = allTextPrinted || !lineInCurrentBlock
    
    def update() = {
      // Do this update if:
      //   - Text is shown gradually
      //   - We haven't exhausted all the text
      //   - The lineI is in the current block
      if(msPerChar > 0 && !allTextPrinted && lineInCurrentBlock) {
        while(timeSinceLastChar > msPerChar) {
          val line = text(lineI)
     
          charI += 1
          
          // advance line if:
          //  - Out of characters
          //  - It's not the last line of the block
          if(charI >= line.length()) {
            lineI += 1
            charI = 0
          }
          
          lastCharPrintTime = System.currentTimeMillis()
        }
      }
    }
    
    def render(b: SpriteBatch) = {
      // Draw all complete lines in current block
      for(i <- blockI*linesPerBlock to (lineI-1)) {
        val idxInBlock = i%linesPerBlock
        drawText(b, text(i), 0, idxInBlock*lineHeight)
      }
      
      // Draw the currently writing line
      if(lineI < text.length) {
        val idxInBlock = lineI%linesPerBlock
        drawText(b, text(lineI).take(charI), 0, idxInBlock*lineHeight)
      }
      
      // If waiting for user input to finish, draw the arrow
      if(drawAwaitingArrow && awaitingInput) {
        skin.drawArrow(b, skinRegion,
            x+w/2-8, y+h-16, 16, 16)
      }
    }
  }
  
  def changeState(newState: Int) = {
    state = newState
    stateStarttime = System.currentTimeMillis()
  }
  
  override def keyDown(key: Int) = {
    import MyKeys._
    if(key == A) {
      // If we have already printed the last line, set to closing
      // otherwise, advance the block.
      
      if(textImage.lineI < text.length) {
        textImage.blockI += 1
      } else {
        changeState(Window.Closing)
      }
      true
    } else {
      false
    }
  }
  
  def update(delta: Float, acceptInput: Boolean) = {
    // change state of "expired" opening or closing animations
    if(stateAge >= openCloseMs) {
      state match {
        case Window.Opening => changeState(Window.Open)
        case Window.Open => textImage.update()
        case Window.Closing => postClose()
        case _ => Unit
      } 
    }
  }
  
  def render(b: SpriteBatch) = state match {
    case Window.Open => {
      skin.draw(b, skinRegion, x, y, w, h)
      textImage.render(b)
    }
    case Window.Opening => {
      val hVisible = 
        math.max(32+(stateAge.toDouble/openCloseMs*(h-32)).toInt, 32)
      
      skin.draw(b, skinRegion, x, y, w, hVisible)
    }
    case Window.Closing => {
      val hVisible = 
        math.max(h-(stateAge.toDouble/openCloseMs*(h-32)).toInt, 32)
      
      skin.draw(b, skinRegion, x, y, w, hVisible)
    }
  }
  
  def postClose() = {
    result.success(0)
  }
  
  // This is used to either convey a choice, or simply that the window
  // has been closed
  val result = Promise[Int]()
}
