package rpgboss.player

import com.badlogic.gdx.graphics.g2d._
import rpgboss.model._
import rpgboss.model.resource._

object GdxGraphicsUtils {
  def renderSprite(
    batch: SpriteBatch, atlasSprites: TextureAtlas, spriteset: Spriteset,
    spriteIdx: Int, dir: Int, step: Int, x: Float, y: Float, w: Float, 
    h: Float) = {
    assume(spriteset != null)
    
    val region = atlasSprites.findRegion(spriteset.name)
    val (srcX, srcY) = spriteset.srcTexels(spriteIdx, dir, step)

    /*
     * Given the definition of the position (see beginning of the file),
     * calculate the top-left corner of the graphic we draw.
     * We use top-left because we have flipped the y-axis in libgdx to match
     * the map coordinates we use.
     */
    val dstOriginX: Float = x - w / 2.0f
    val dstOriginY: Float = y - h + w / 2

    val srcXInRegion = region.getRegionX() + srcX
    val srcYInRegion = region.getRegionY() + srcY

    batch.draw(
      region.getTexture(),
      dstOriginX.toFloat,
      dstOriginY.toFloat,
      w, h,
      srcXInRegion,
      srcYInRegion,
      spriteset.tileW,
      spriteset.tileH,
      false, true)
  }
}