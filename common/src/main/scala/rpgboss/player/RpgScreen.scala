package rpgboss.player

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import rpgboss.lib.ThreadChecked
import rpgboss.model.Project
import rpgboss.model.SoundSpec
import rpgboss.model.resource.Music
import rpgboss.model.resource.MusicPlayer
import rpgboss.model.resource.RpgAssetManager
import rpgboss.model.resource.Sound
import rpgboss.model.resource.SoundPlayer
import rpgboss.model.AnimationSound
import rpgboss.model.Animation
import rpgboss.player.entity.AnimationPlayer

object RpgScreen {
  val MAX_MUSIC_SLOTS = 8
}

trait RpgScreen extends Screen with ThreadChecked {
  def project: Project
  def assets: RpgAssetManager
  def screenW: Int
  def screenH: Int

  def scriptInterface: ScriptInterface

  val inputs = new InputMultiplexer()
  def createWindowManager(): WindowManager =
    new WindowManager(assets, project, screenW, screenH)

  val musics = Array.fill[Option[MusicPlayer]](RpgScreen.MAX_MUSIC_SLOTS)(None)

  val windowManager = createWindowManager()

  val animationManager = new AnimationManager()

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
    loop: Boolean, fadeDuration: Float): Unit = {
    assertOnBoundThread()

    if (slot < 0 || slot >= RpgScreen.MAX_MUSIC_SLOTS)
      return

    musics(slot).map({ oldMusic =>
      oldMusic.volumeTweener.tweenTo(0f, fadeDuration)
      oldMusic.volumeTweener.runAfterDone(() => {
        oldMusic.stop()
      })
    })

    musics(slot) = specOpt.map { spec =>
      val resource = Music.readFromDisk(project, spec.sound)
      val newMusic = resource.newPlayer(assets)

      // Start at zero volume and fade to desired volume
      newMusic.stop()
      newMusic.setVolume(0f)
      newMusic.setLooping(loop)
      newMusic.play()
      newMusic.volumeTweener.tweenTo(spec.volume, fadeDuration)

      newMusic
    }
  }

  def playSound(soundSpec: SoundSpec): Unit = {
    val animationSound = AnimationSound(0.0f, soundSpec)
    val animation = Animation(sounds = Array(animationSound))
    val player = new AnimationPlayer(project, animation, assets, 0f, 0f)
    animationManager.addAnimation(player)
    player.play()
  }

  def render()
  def update(delta: Float)

  def reset() = {
    windowManager.reset()
    musics.foreach(_.map(music => {
      music.stop()
      music.dispose()
    }))
  }

  override def dispose() = {
    windowManager.dispose()

    animationManager.dispose()

    musics.foreach(_.map(music => {
      music.stop()
      music.dispose()
    }))
  }

  override def hide() = {
    assertOnBoundThread()
    inputs.releaseAllKeys()
    Gdx.input.setInputProcessor(null)

    // Sholud start all black again
    windowManager.transitionAlpha = 1.0f

    musics.foreach(_.map(_.pause()))
  }

  override def pause() = {
    assertOnBoundThread()
  }

  override def render(delta: Float): Unit = {
    assertOnBoundThread()

    if (!assets.update())
      return

    musics.foreach(_.map(_.update(delta)))

    // Update tweens
    windowManager.update(delta)

    animationManager.update(delta)

    if (!windowManager.inTransition)
      update(delta)

    render()
  }

  override def resize(width: Int, height: Int) = {
    assertOnBoundThread()
    // Do nothing for now
  }

  override def resume() = {
    assertOnBoundThread()
  }

  override def show() = {
    assertOnBoundThread()

    Gdx.input.setInputProcessor(inputs)
    musics.foreach(_.map(_.play()))
  }
}

trait RpgScreenWithGame extends RpgScreen {
  def game: RpgGame

  def project = game.project
  def screenW = project.data.startup.screenW
  def screenH = project.data.startup.screenH
  def assets = game.assets
  val scriptInterface = new ScriptInterface(game, this)
  val scriptFactory = new ScriptThreadFactory(scriptInterface)

}
