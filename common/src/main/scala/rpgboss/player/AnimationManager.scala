package rpgboss.player

import rpgboss.lib.ThreadChecked
import rpgboss.player.entity.AnimationPlayer
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable

class AnimationManager extends ThreadChecked with Disposable {
  val animations = new collection.mutable.HashSet[AnimationPlayer]

  def addAnimation(animation: AnimationPlayer) = {
    animations += animation
  }

  def dispose() = {
    animations.foreach(_.dispose())
  }

  def update(delta: Float) = {
    animations.foreach(_.update(delta))

    val toRemove = new collection.mutable.HashSet[AnimationPlayer]

    for (animation <- animations) {
      animation.update(delta)

      if (animation.done)
        toRemove += animation
    }

    toRemove.foreach(_.dispose())
    animations --= toRemove
  }

  def render(batch: SpriteBatch) = {
    animations.foreach(_.render(batch))
  }
}