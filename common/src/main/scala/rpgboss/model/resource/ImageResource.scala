package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.lib.FileHelper._
import java.io._
import java.awt.image._
import javax.imageio._
import java.awt.Graphics
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.Gdx

trait TiledImageResource[T, MT <: AnyRef] extends ImageResource[T, MT] {
  def tileH: Int
  def tileW: Int
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
}

trait ImageResource[T, MT <: AnyRef]
  extends Resource[T, MT]
  with RpgGdxAsset[Texture] {
  // TODO: Refactor or eliminate this method, since it's unpredictable
  lazy val img = Option(ImageIO.read(newDataStream)) getOrElse {
    throw ResourceException("Can't load window skin: %s".format(name))
  }
}

object ImageResource {
  lazy val errorTile = rpgboss.lib.Utils.readClasspathImage("error.png")
}
