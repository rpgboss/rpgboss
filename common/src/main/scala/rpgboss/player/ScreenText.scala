package rpgboss.player

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.Color

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont

import rpgboss.model.resource._
import scala.collection.mutable.Map

object ScreenTextMsgfontStorage {

  var fontMap:Map[String,BitmapFont] = Map[String,BitmapFont]()

  def generateFont(id:Int, size:Int,msgfont:Msgfont):BitmapFont = {

    var key = id.toString+size.toString

    if(fontMap.keySet.exists(_ == key)) {
      return fontMap(key)
    } else {

      var generator = new FreeTypeFontGenerator(msgfont.getGdxFileHandle);
      var parameter = new FreeTypeFontParameter();
      parameter.size = size;
      parameter.flip = true;

      generator.scaleForPixelHeight(size);
      parameter.minFilter = Texture.TextureFilter.Nearest;
      parameter.magFilter = Texture.TextureFilter.MipMapLinearNearest;
      var font = generator.generateFont(parameter);
      generator.dispose();

      fontMap.update(key,font)

      return font

    }
  }
}

class ScreenText(val id:Int,val text:String,val x:Int,val y:Int, val color:Color, val size:Int) {

  def dispose(manager: WindowManager) {

  }
  def render(manager: WindowManager, batch: SpriteBatch, msgfont: Msgfont) {


      var font = ScreenTextMsgfontStorage.generateFont(id, size,msgfont)
      font.setColor(color)
      font.draw(batch, text, x, y)
  }
}