package rpgboss.player.entity

import com.badlogic.gdx.utils.Disposable
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Can only be used from the Gdx thread.
 */
class AnimationPlayer(
  proj: Project, animation: Animation, assets: RpgAssetManager)
  extends Disposable {

  // Load all the assets used in this animation.
  val animationImages = animation.visuals.map(
    v => AnimationImage.readFromDisk(proj, v.animationImage))
  val animationSounds = animation.sounds.map(
    s => Sound.readFromDisk(proj, s.sound.sound))
  animationImages.map(_.loadAsset(assets))
  animationSounds.map(_.loadAsset(assets))

  /**
   *  Time of the previous update call.
   */
  private var _prevTime = 0.0f

  private var _time = 0.0f
  private var _playing = false
  val totalTime = animation.totalTime

  def time = _time
  def playing = _playing

  def play() = _playing = true

  def update(delta: Float): Unit = {
    if (_playing) {
      _prevTime = _time
      _time += delta
      if (_time >= totalTime) {
        _prevTime = 0f
        _time = 0f
        _playing = false
      }
    }
  }

  /**
   * Assumes |batch| is already centered on the animation origin.
   */
  def render(batch: SpriteBatch) = {
    import TweenUtils._
    for ((visual, image) <- animation.visuals zip animationImages) {
      if (visual.within(time) && image.isLoaded(assets)) {
        val alpha = tweenAlpha(visual.start.time, visual.end.time, time)
        val frameIndex = tweenIntInclusive(
          alpha, visual.start.frameIndex, visual.end.frameIndex)
        val x = tweenFloat(alpha, visual.start.x, visual.end.x)
        val y = tweenFloat(alpha, visual.start.y, visual.end.y)

        val texture = image.getAsset(assets)
        batch.draw(
          texture, x - texture.getWidth() / 2, y - texture.getWidth() / 2)
      }
    }

    for ((animationSound, sound) <- animation.sounds zip animationSounds) {
      if (animationSound.time >= _prevTime && animationSound.time < time &&
          sound.isLoaded(assets)) {
        val soundSpec = animationSound.sound
        sound.getAsset(assets).play(soundSpec.volume, soundSpec.pitch, 0f)
      }
    }
  }

  def dispose() = {
    animationImages.map(_.unloadAsset(assets))
    animationSounds.map(_.unloadAsset(assets))
  }
}