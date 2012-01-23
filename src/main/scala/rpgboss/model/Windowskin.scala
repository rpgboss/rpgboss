package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.Utils._
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
    
    // paint stretch background 
    val stretchBg = img.subimage(0, 0, 64, 64)
    g.drawImage(stretchBg, 0, 0, w, h, null)
    
    // paint tiled background
    val tileBg = img.subimage(0, 64, 64, 64)
    for(i <- 0 until ceilIntDiv(w, 64); j <- 0 until ceilIntDiv(h, 64))
      g.drawImage(tileBg, i*64, j*64, null)
    
    // paint borders
    def subimg16(x: Int, y: Int, w: Int, h: Int) =
      img.subimage(x*16, y*16, w*16, h*16)
    
    g.drawImage(subimg16(4, 0, 1, 1), 0   , 0   , null) // NW
    g.drawImage(subimg16(7, 0, 1, 1), w-16, 0   , null) // NE
    g.drawImage(subimg16(4, 3, 1, 1), 0   , h-16, null) // SW
    g.drawImage(subimg16(7, 3, 1, 1), w-16, h-16, null) // SE
    
    g.drawImage(subimg16(5, 0, 2, 1), 16  , 0   , w-32, 16  , null) // N
    g.drawImage(subimg16(5, 3, 2, 1), 16  , h-16, w-32, 16  , null) // S
    g.drawImage(subimg16(4, 1, 1, 2), 0   , 16  , 16  , h-32, null) // E
    g.drawImage(subimg16(7, 1, 1, 2), w-16, 16  , 16  , h-32, null) // W
    
    g.dispose()
    
    canvasImg
  } 
}

object Windowskin extends MetaResource[Windowskin, WindowskinMetadata] {
  def rcType = "windowskin"
  def keyExts = Array("png")
  
  def defaultInstance(proj: Project, name: String) = 
    Windowskin(proj, name, WindowskinMetadata())
}
