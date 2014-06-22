package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._
import java.io._
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont

case class MsgfontMetadata()

case class Msgfont(proj: Project, name: String,
                   metadata: MsgfontMetadata)
  extends Resource[Msgfont, MsgfontMetadata] {
  def meta = Msgfont

  def getBitmapFont(): BitmapFont = {
    val generator = new FreeTypeFontGenerator(getGdxFileHandle)

    val params = new FreeTypeFontGenerator.FreeTypeFontParameter
    params.size = proj.data.startup.fontsize
    params.flip = true
    val result = generator.generateFont(params)

    generator.dispose()

    result
  }
}

object Msgfont extends MetaResource[Msgfont, MsgfontMetadata] {
  def rcType = "msgfont"
  def keyExts = Array("ttf")

  def defaultInstance(proj: Project, name: String) =
    Msgfont(proj, name, MsgfontMetadata())
}
