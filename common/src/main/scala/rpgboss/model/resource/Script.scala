package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._
import java.io._
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont

case class ScriptMetadata()

case class Script(proj: Project, name: String,
                  metadata: ScriptMetadata)
  extends Resource[Script, ScriptMetadata] {
  def meta = Script
  def getAsString = dataFile.readAsString.getOrElse("")
}

object Script extends MetaResource[Script, ScriptMetadata] {
  def rcType = "script"
  def keyExts = Array("js")

  def defaultInstance(proj: Project, name: String) =
    Script(proj, name, ScriptMetadata())
}
