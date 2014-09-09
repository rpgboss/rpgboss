package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._
import java.io._
import javax.sound.midi._
import com.badlogic.gdx.audio.{ Music => GdxMusic }
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.utils.Disposable
import com.google.common.io.Files
import com.typesafe.scalalogging.slf4j.LazyLogging

case class MusicMetadata()

case class Music(
  proj: Project,
  name: String,
  metadata: MusicMetadata)
  extends Resource[Music, MusicMetadata]
  with RpgGdxAsset[GdxMusic] {
  def meta = Music

  def isMidi = {
    val extension = Files.getFileExtension(name)
    extension == "midi" || extension == "mid"
  }

  def newPlayer(assets: RpgAssetManager) = {
    if (isMidi)
      new MidiMusicPlayer(this)
    else {
      new GdxMusicPlayer(assets, this)
    }
  }
}

object Music extends MetaResource[Music, MusicMetadata] {
  def rcType = "music"
  def keyExts = Array("wav", "mp3", "ogg", "midi", "mid")

  def defaultInstance(proj: Project, name: String) =
    Music(proj, name, MusicMetadata())
}

trait MusicPlayer extends Disposable {
  def getVolume(): Float
  def setVolume(newVolume: Float)
  def pause()
  def play()
  def setLooping(loop: Boolean)
  def stop()
  def dispose()
}

class MidiMusicPlayer(music: Music) extends MusicPlayer with LazyLogging {
  val sequencer: Sequencer = try {
    MidiSystem.getSequencer()
  } catch {
    case _: Throwable => {
      logger.error("Could not initialize MIDI sequencer")
      null
    }
  }

  val sequence: Sequence = try {
    val s = MidiSystem.getSequence(music.newDataStream)
    if (sequencer != null) {
      sequencer.open()
      sequencer.setSequence(s)
    }
    s
  } catch {
    case _: Throwable => {
      logger.error("Could not initialize MIDI sequence")
      null
    }
  }

  def getVolume() = {
    logger.warn("MIDI getVolume() not implemented.")
    1.0f
  }

  def setVolume(newVolume: Float) = {
//    logger.warn("MIDI setVolume() not implemented.")
  }

  def pause() = {
    if (sequencer != null && sequencer.isOpen()) {
      sequencer.stop()
    } else {
      logger.warn("MIDI sequencer null or not open")
    }
  }

  def play() = {
    if (sequencer != null && sequencer.isOpen()) {
      sequencer.start()
    } else {
      logger.warn("MIDI sequencer null or not open")
    }
  }

  def setLooping(loop: Boolean) = {
    if (sequencer != null && sequencer.isOpen()) {
      val count = if (loop) Sequencer.LOOP_CONTINUOUSLY else 0
      sequencer.setLoopCount(count)
    } else {
      logger.warn("MIDI sequencer null or not open")
    }
  }

  def stop() = {
    if (sequencer != null && sequencer.isOpen()) {
      sequencer.stop()
      sequencer.setTickPosition(0)
    } else {
      logger.warn("MIDI sequencer null or not open")
    }
  }

  def dispose() = {
    stop()
    if (sequencer != null)
      sequencer.close()
  }
}

/**
 * Because this can either be a MIDI or a normal libgdx music, there is a
 * special interface. No calls will work until the asset is finished loading.
 */
class GdxMusicPlayer(assets: RpgAssetManager, music: Music)
  extends MusicPlayer {

  // TODO: Converted to use asset manager. Now I'm concerned that there cannot
  // be independent instances of the same piece of music. Wouldn't pausing one
  // instance of the music pause all other playing instances of the same file?

  music.loadAsset(assets)

  def getVolume() = {
    if (music.isLoaded(assets))
      music.getAsset(assets).getVolume()
    else
      0f
  }

  def setVolume(newVolume: Float) = {
    if (music.isLoaded(assets))
      music.getAsset(assets).setVolume(newVolume)
  }

  def setLooping(loop: Boolean) = {
    if (music.isLoaded(assets))
      music.getAsset(assets).setLooping(loop)
  }

  def stop() = {
    if (music.isLoaded(assets))
      music.getAsset(assets).stop()
  }

  def pause() = {
    if (music.isLoaded(assets))
      music.getAsset(assets).pause()
  }

  def play() = {
    if (music.isLoaded(assets))
      music.getAsset(assets).play()
  }

  def dispose() = {
    music.unloadAsset(assets)
  }
}