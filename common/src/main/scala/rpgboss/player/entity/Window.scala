package rpgboss.player.entity

import rpgboss.model._
import rpgboss.model.resource._
import java.awt._
import java.awt.image._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion

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
case class Window(proj: Project,
                  name: String,
                  text: Array[String] = Array(),
                  x: Int, y: Int, w: Int, h: Int,
                  skin: Windowskin,
                  region: TextureRegion,
                  font: Font,
                  var state: Int = Window.Opening, 
                  var stateAge: Int = 0,
                  openCloseFrames: Int = 40,
                  framesPerChar: Int = 7,
                  linesPerBlock: Int = 4,
                  var textBlock: Int = 0,
                  justification: Int = Window.Left) 
extends Entity
{
  // stateAge is used for:
  // - controlling the opening and closing of windows
  
  object textImage extends Entity {
    val textW = w-40
    val textH = h-24
    val lineHeight = 32f
    
    var lineI = 0
    var charI = 0
    
    // these must be initialized later
    var curX = -1f
    var curY = -1f
    
    val imgOpt = if(textW > 0 && textH > 0) {
      val img = new BufferedImage(textW, textH, BufferedImage.TYPE_4BYTE_ABGR)
      val g = img.createGraphics()
      g.setFont(font)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
      //g.drawRect(0, 0, textW-1, textH-1)
      val fm = g.getFontMetrics()
      curY = fm.getHeight()
      Some((img, g, fm))
    } else None
    
    var tickNum = 0
    
    def xStartForJustify(fm: FontMetrics, line: String) = {
      if(justification == Window.Left) 0 else {
        val strWidth = fm.stringWidth(line)
        
        if(justification == Window.Right)
          textW-strWidth
        else
          (textW-strWidth)/2
      }
    }
    
    def update() = imgOpt map { case (img, g, fm) =>
      
      if(tickNum % framesPerChar == 0 && lineI < text.length) {
        val line = text(lineI)
        
        // FIXME: need to adjust width for control characters
        // fix x cursor start to accomodate justifications
        if(charI == 0) {
          curX = xStartForJustify(fm, line)
        }
        
        // FIXME: Right now doesn't handle variables, character names,
        // or any control characters for that matter
        var charStr = line.substring(charI, charI+1)
        println("g.drawString(%s, %f, %f)".format(charStr, curX, curY))
        g.setColor(Color.BLACK)
        g.drawString(charStr, curX+2, curY+2)
        g.setColor(Color.WHITE)
        g.drawString(charStr, curX, curY)
        charI += 1
        curX += fm.stringWidth(charStr)
        
        // advance line if out of characters
        if(charI >= line.length()) {
          lineI += 1
          charI = 0
          
          curX = 0 // will be readjusted on next update
          curY += lineHeight // advance line height
        }
        
      }
      
      tickNum += 1      
    }
    
    /*def render(g: Graphics2D) = imgOpt map { case (img, _, _) =>
      g.drawImage(img, x+20, y+12, null)
    }*/
    def render(b: SpriteBatch) = {}
  }
  
  def update() = {
    // change state of "expired" opening or closing animations
    if(stateAge >= openCloseFrames) {
      state match {
        case Window.Opening => 
          state = Window.Open
          stateAge = 0
        case Window.Open => textImage.update()
        case Window.Closing => delete()
        case _ => Unit
      } 
    }
    
    // increase stateAge of every window
    stateAge += 1
  }
  
  def render(b: SpriteBatch) = state match {
    case Window.Open => {
      skin.draw(b, region, x, y, w, h)
      textImage.render(b)
    }
    case Window.Opening => {
      val hVisible = 
        math.max(32+(stateAge.toDouble/openCloseFrames*(h-32)).toInt, 32)
      
      val wVisible = 
        math.max(32+(stateAge.toDouble/openCloseFrames*(w-32)).toInt, 32)     
      
      skin.draw(b, region, x, y, wVisible, hVisible)
    }
  }
}
