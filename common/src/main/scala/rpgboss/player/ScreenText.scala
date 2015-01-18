package rpgboss.player

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Color

class ScreenText(val id:Int,val text:String,val x:Int,val y:Int, val color:Color, val scale:Float) {

  def dispose(manager: WindowManager) {

  }
  def render(manager: WindowManager, batch: SpriteBatch) {
      var font = manager.fontbmp;
      var savedScale = font.getScaleX()
      font.setScale(scale)
      font.setColor(color)
      font.draw(batch, text, x, y)
      font.setScale(savedScale)
  }
}