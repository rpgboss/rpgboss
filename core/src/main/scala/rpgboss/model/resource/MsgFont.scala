package rpgboss.model.resource

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

import rpgboss.model.Project

case class MsgfontMetadata()

case class Msgfont(proj: Project, name: String,
                   metadata: MsgfontMetadata)
  extends Resource[Msgfont, MsgfontMetadata] {
  def meta = Msgfont

  def getBitmapFont(distinctChars: String): BitmapFont = {
    val generator = new FreeTypeFontGenerator(getGdxFileHandle)

    val params = new FreeTypeFontGenerator.FreeTypeFontParameter
    params.size = proj.data.startup.fontsize
    params.flip = true
    params.shadowColor = Color.BLACK
    params.shadowOffsetX = 2
    params.shadowOffsetY = 2

    if (!distinctChars.isEmpty()) {
      params.characters =
        (FreeTypeFontGenerator.DEFAULT_CHARS + distinctChars).distinct
    }

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
