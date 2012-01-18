package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import java.awt.image.BufferedImage

case class WindowskinMetadata()

case class Windowskin(proj: Project, name: String, 
                      metadata: WindowskinMetadata)
extends TiledImageResource[Windowskin, WindowskinMetadata]
{
  def meta = Windowskin
  def tileH = 16
  def tileW = 16
    
  // in pixels
  def windowImage(w: Int, h: Int) = {
    val canvasImg = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR)
    val g = canvasImg.getGraphics()
    
    // stretch background 
    
  } 
}

object Windowskin extends MetaResource[Windowskin, WindowskinMetadata] {
  def rcType = "windowskin"
  def keyExt = "png"
  
  def defaultInstance(proj: Project, name: String) = 
    Windowskin(proj, name, WindowskinMetadata())
}
