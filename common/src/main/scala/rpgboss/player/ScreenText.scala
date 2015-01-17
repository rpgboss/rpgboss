package rpgboss.player

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._

class ScreenText(val id:Int,val text:String,val x:Int,val y:Int) {
	
  def dispose(manager: WindowManager) {

  }
  def render(manager: WindowManager, batch: SpriteBatch) {
      var font = manager.fontbmp;
      font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
      font.draw(batch, text, x, y);
  }
}