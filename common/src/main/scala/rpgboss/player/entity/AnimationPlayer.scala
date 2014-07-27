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

  /**
   * Request all the assets used in this animation.
   */
  for (visual <- animation.visuals) {
    val animationImage =
      AnimationImage.readFromDisk(proj, visual.animationImage)
  }

  def update(delta: Float) = {

  }

  /**
   * Assumes |batch| is already centered on the animation origin.
   */
  def render(batch: SpriteBatch) = {

  }

  def dispose() = {

  }
}