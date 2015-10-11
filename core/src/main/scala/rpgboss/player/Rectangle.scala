package rpgboss.player

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.Gdx

class Rectangle(val id:Int,val x:Int,val y:Int, val width:Int,val height:Int,val color:Color=new Color(255, 255, 255, 1),val recttype:ShapeType=ShapeType.Filled) {

  def dispose(manager: WindowManager) {

  }
  def render(manager: WindowManager, batch: SpriteBatch, shapeRenderer:ShapeRenderer) {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapeRenderer.begin(recttype);
    shapeRenderer.setColor(color);
    shapeRenderer.rect(x, y, width, height);
    shapeRenderer.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }
}

class RoundedRectangle(override val id:Int,val radius:Int,override val x:Int,override val y:Int, override val width:Int,override val height:Int,override val color:Color=new Color(255, 255, 255, 1),override val recttype:ShapeType=ShapeType.Filled) 
  extends Rectangle(id, x,y,width,height,color,recttype) {

  override def render(manager: WindowManager, batch: SpriteBatch, shapeRenderer:ShapeRenderer) {

    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapeRenderer.begin(recttype);
    shapeRenderer.setColor(color);
    shapeRenderer.rect(x+0, y+radius, width, height-2*radius);
    shapeRenderer.rect(x+radius, y+0, width-2*radius, height);
    shapeRenderer.circle(x+ radius, y+ radius, radius);
    shapeRenderer.circle(x+radius, y+ height-radius, radius);
    shapeRenderer.circle(x+ width-radius, y+ radius, radius);
    shapeRenderer.circle(x+ width-radius, y+ height-radius, radius);
    shapeRenderer.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }
}