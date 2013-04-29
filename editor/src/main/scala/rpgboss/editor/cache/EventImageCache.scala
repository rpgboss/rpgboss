package rpgboss.editor.cache

import rpgboss.editor.StateMaster
import com.google.common.cache._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage

class EventImageCache(sm: StateMaster)
  extends CacheLoader[SpriteSpec, BufferedImage] {
  val cache = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .softValues()
    .maximumSize(50)
    .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
    .build(this)

  def load(spriteSpec: SpriteSpec) = {
    val spriteset = Spriteset.readFromDisk(sm.getProj, spriteSpec.spriteset)
    val srcImg = spriteset.srcTileImg(spriteSpec)

    val dstSz = Tileset.tilesize - 4 - 1

    val dstImg = new BufferedImage(
      dstSz, dstSz, BufferedImage.TYPE_4BYTE_ABGR)

    val g = dstImg.getGraphics()

    val sx1 = (srcImg.getWidth() - dstSz) / 2
    val sy1 = 10
    g.drawImage(srcImg,
      0, 0,
      dstSz - 1, dstSz - 1,
      sx1, sy1,
      sx1 + dstSz - 1, sy1 + dstSz - 1,
      null)

    dstImg
  }

  def get(s: SpriteSpec) = cache.get(s)
}