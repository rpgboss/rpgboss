package rpgboss.player

import com.badlogic.gdx.graphics.g2d._
import rpgboss.model._
import rpgboss.model.resource._

object GdxGraphicsUtils {
  /**
   * x and y are defined to the top-left of the sprite image.
   */
  def renderSprite(
    batch: SpriteBatch, atlasSprites: TextureAtlas, spriteset: Spriteset,
    spriteIdx: Int, dir: Int, step: Int, x: Float, y: Float, w: Float, 
    h: Float) = {
    assume(spriteset != null)
    
    val region = atlasSprites.findRegion(spriteset.name)
    val (srcX, srcY) = spriteset.srcTexels(spriteIdx, dir, step)

    val srcXInRegion = region.getRegionX() + srcX
    val srcYInRegion = region.getRegionY() + srcY

    batch.draw(
      region.getTexture(),
      x, y, w, h,
      srcXInRegion,
      srcYInRegion,
      spriteset.tileW,
      spriteset.tileH,
      false, true)
  }
}