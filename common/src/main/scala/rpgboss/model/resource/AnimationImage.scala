package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.FileHelper._

import org.json4s.native.Serialization

case class AnimationImageMetadata()

/**
 * This class specifies the resource only.
 */
case class AnimationImage(proj: Project,
                          name: String,
                          metadata: AnimationImageMetadata)
  extends TiledImageResource[AnimationImage, AnimationImageMetadata] {
  def meta = AnimationImage

  val (tileH, tileW) = (AnimationImage.tilesize, AnimationImage.tilesize)

  def getImageForFrame(frameIndex: Int) = {
    assume(frameIndex >= 0)

    val ti = frameIndex % xTiles
    val tj = frameIndex / xTiles

    logger.debug("name=%s, xTiles=%d, yTiles=%d".format(name, xTiles, yTiles))
    logger.debug("frameIndex=%d, ti=%d, tj=%d".format(frameIndex, ti, tj))
    assert(ti < xTiles)
    assert(tj < yTiles)
    getTileImage(ti, tj)
  }
}

object AnimationImage
  extends MetaResource[AnimationImage, AnimationImageMetadata] {
  def rcType = "animation"
  def keyExts = Array("png")

  def tilesize = 192

  def defaultInstance(proj: Project, name: String) =
    AnimationImage(proj, name, AnimationImageMetadata())
}

