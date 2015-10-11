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
import com.badlogic.gdx.utils.Disposable

case class SoundMetadata()

case class Sound(
  proj: Project,
  name: String,
  metadata: SoundMetadata)
  extends Resource[Sound, SoundMetadata]
  with RpgGdxAsset[com.badlogic.gdx.audio.Sound] {
  def meta = Sound
}

object Sound extends MetaResource[Sound, SoundMetadata] {
  def rcType = "sound"
  def keyExts = Array("wav", "mp3", "ogg")

  def defaultInstance(proj: Project, name: String) =
    Sound(proj, name, SoundMetadata())
}

class SoundPlayer(assets: RpgAssetManager, sound: Sound, soundSpec: SoundSpec)
  extends Disposable {
  sound.loadAsset(assets)

  def play() = {
    assert(sound.isLoaded(assets))
    val gdxSound = sound.getAsset(assets)
    gdxSound.play(soundSpec.volume, soundSpec.pitch, 0f /* pan */)
  }

  def failed = sound.failed
  def isLoaded = sound.isLoaded(assets)

  def dispose() = {
    sound.dispose(assets)
  }
}
