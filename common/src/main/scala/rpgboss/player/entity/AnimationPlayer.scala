package rpgboss.player.entity

import com.badlogic.gdx.utils.Disposable
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

  private var _time = 0.0f
  private var _playing = false
  val totalTime = animation.totalTime

  def time = _time
  def playing = _playing

  def play() = _playing = true

  def update(delta: Float): Unit = {
    if (_playing) {
      _time += delta
      if (_time >= totalTime) {
        _time = 0f
        _playing = false
      }
    }
  }

  /**
   * Assumes |batch| is already centered on the animation origin.
   */
  def render(batch: SpriteBatch) = {

  }

  def dispose() = {
    animationImages.map(_.unloadAsset(assets))
    animationSounds.map(_.unloadAsset(assets))
  }
}