package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._
import java.io._
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.assets.AssetManager

case class MusicMetadata()

case class Music(
  proj: Project,
  name: String,
  metadata: MusicMetadata)
  extends Resource[Music, MusicMetadata]
  with RpgGdxAsset[com.badlogic.gdx.audio.Music] {
  def meta = Music
}

object Music extends MetaResource[Music, MusicMetadata] {
  def rcType = "music"
  def keyExts = Array("wav", "mp3", "ogg")

  def defaultInstance(proj: Project, name: String) =
    Music(proj, name, MusicMetadata())
}
