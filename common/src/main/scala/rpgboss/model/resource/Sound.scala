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

case class SoundMetadata()

case class Sound(
    proj: Project, 
    name: String, 
    metadata: SoundMetadata)
  extends Resource[Sound, SoundMetadata]
  with RpgGdxAsset[com.badlogic.gdx.audio.Sound] 
{
  def meta = Sound
}

object Sound extends MetaResource[Sound, SoundMetadata] {
  def rcType = "sound"
  def keyExts = Array("wav", "mp3", "ogg")
  
  def defaultInstance(proj: Project, name: String) = 
    Sound(proj, name, SoundMetadata())
}
