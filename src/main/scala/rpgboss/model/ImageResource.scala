package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import java.io._
import java.awt.image._
import javax.imageio._

// Wrapper class in case I need to tweak subimage to speed things up
case class FastImage(img: BufferedImage) {
  // FIXME: I believe this destroys the HW acceleration
  def subimage(x: Int, y: Int, w: Int, h: Int) = {
    val sub = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR)
    val g = sub.createGraphics()
    g.drawImage(img, 0, 0, w, h,
                     x, y, x+w, y+h, null)
    g.dispose()
    sub
    //img.getSubimage(x, y, w, h)
  }
  
  def width = img.getWidth()
  def height = img.getHeight()
}

trait TiledImageResource[T, MT <: AnyRef] extends ImageResource[T, MT] {
  def tileH: Int
  def tileW: Int
  def name: String
  
  // FIXME: May need to optimize if drawing performance sucks
  def getTile(ti: Int, tj: Int) : BufferedImage = {
    if(ti < img.width/tileH && tj < img.height/tileH) {
      img.subimage(ti*tileW, tj*tileH, tileW, tileH)
    } else {
      throw ResourceException("Requested tile out of bounds. " +
                              "(name=%s, ti=%d, tj=%d)".format(name, ti, tj))
    }
  }
}

trait ImageResource[T, MT <: AnyRef] extends Resource[T, MT] {  
  lazy val img = Option(ImageIO.read(dataFile)).map(FastImage) getOrElse {
    throw ResourceException("Can't load window skin: %s".format(name))
  }
}

object ImageResource {
  lazy val errorTile = { 
    val errImgStream = getClass.getClassLoader.getResourceAsStream("error.png")
    ImageIO.read(errImgStream)
  }
}
