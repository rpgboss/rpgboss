package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._
import java.awt.image._
import javax.imageio._

trait ImageResource[T] extends Resource[T] {
  def imgFile = new File(rcTypeDir, "%s.png".format(name))
  
  def getImg() = Option(ImageIO.read(imgFile))
}
