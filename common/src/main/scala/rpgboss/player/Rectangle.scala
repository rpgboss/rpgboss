package rpgboss.player

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

class Rectangle(val id:Int,val x:Int,val y:Int, val width:Int,val height:Int,val color:Color=new Color(255, 255, 255, 1),val recttype:ShapeType=ShapeType.Filled) {

  def dispose(manager: WindowManager) {

  }
  def render(manager: WindowManager, batch: SpriteBatch, shapeRenderer:ShapeRenderer) {
    shapeRenderer.begin(recttype);
    shapeRenderer.setColor(color);
    shapeRenderer.rect(x, y, width, height);
    shapeRenderer.end();
  }
}