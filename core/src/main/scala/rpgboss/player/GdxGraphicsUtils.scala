package rpgboss.player

import com.badlogic.gdx.graphics.g2d._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.Texture

object GdxGraphicsUtils {
  def drawCentered(batch: SpriteBatch, texture: Texture, x: Float, y: Float) = {
    batch.draw(texture, x - texture.getWidth() / 2, y - texture.getHeight() / 2)
  }
}