package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.lib.FileHelper._
import java.io._
import java.awt.image._
import javax.imageio._
import java.awt.Graphics
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch

trait TiledImageResource[T, MT <: AnyRef] extends ImageResource[T, MT] {
  def tileH: Int
  def tileW: Int

  lazy val xTiles = img.getWidth() / tileW
  lazy val yTiles = img.getHeight() / tileH

  def inBounds(xTile: Int, yTile: Int) =
    xTile >= 0 && xTile < xTiles && yTile >= 0 && yTile < yTiles

  def name: String

  def drawTileAt(ti: Int, tj: Int, g: Graphics, dstX: Int, dstY: Int) = {
    if (ti < img.getWidth() / tileW && tj < img.getHeight() / tileH) {
      g.drawImage(img,
        dstX, dstY,
        dstX + tileW, dstY + tileH,
        ti * tileW, tj * tileH,
        ti * tileW + tileW, tj * tileH + tileH,
        null)
    } else {
      throw ResourceException("Requested tile out of bounds. " +
        "(name=%s, ti=%d, tj=%d)".format(name, ti, tj))
    }
  }

  def getTileImage(ti: Int, tj: Int) = {
    if (ti < img.getWidth() / tileW && tj < img.getHeight() / tileH) {
      img.getSubimage(ti * tileW, tj * tileH, tileW, tileH)
    } else {
      throw ResourceException("Requested tile out of bounds. " +
        "(name=%s, ti=%d, tj=%d)".format(name, ti, tj))
    }
  }

  def srcTexels(xTile: Int, yTile: Int): (Int, Int) =
    (xTile * tileW, yTile * tileH)

  /**
   * @param   texture   Must be the texture containing this tiled image.
   */
  def drawTileAt(
    batch: SpriteBatch, texture: Texture,
    dstX: Float, dstY: Float, dstW: Float, dstH: Float,
    xTile: Int, yTile: Int,
    texelXOffset: Int = 0, texelYOffset: Int = 0) = {
    val srcXOffsetted = texelXOffset + xTile * tileW
    val srcYOffsetted = texelYOffset + yTile * tileH

    batch.draw(
      texture,
      dstX, dstY, dstW, dstH,
      srcXOffsetted,
      srcYOffsetted,
      tileW,
      tileH,
      false, true)
  }

  def drawTileCentered(
    batch: SpriteBatch, assets: RpgAssetManager,
    dstX: Float, dstY: Float,
    xTile: Int, yTile: Int,
    sizeScale: Float) {
    assert(isLoaded(assets))
    val texture = getAsset(assets)
    drawTileAt(
      batch, texture,
      dstX - tileW / 2f, dstY - tileH / 2f,
      tileW * sizeScale, tileH * sizeScale,
      xTile, yTile)
  }
}

trait ImageResource[T, MT <: AnyRef]
  extends Resource[T, MT]
  with RpgGdxAsset[Texture] {
  // TODO: Refactor or eliminate this method, since it's unpredictable
  lazy val img = Option(ImageIO.read(newDataStream)) getOrElse {
    throw ResourceException("Can't load image: %s".format(name))
  }
}

object ImageResource {
  lazy val errorTile = rpgboss.lib.Utils.readClasspathImage("error.png")
}
