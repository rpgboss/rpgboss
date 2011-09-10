package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._

import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

import rpgboss.model._
import rpgboss.message._

import javax.imageio.ImageIO
import java.io._

import java.awt.image.BufferedImage
import java.awt.image.BufferedImage._
/*
class TilesetEditPanel(val mainP: MainPanel, tsResp: TilesetResp)
extends GridBagPanel
{
  import Tileset.tilesize
  
  val metadata = tsResp.metadata
  
  def xPixels = metadata.xTiles*tilesize
  def yPixels = metadata.yTiles*tilesize
  
  minimumSize = new Dimension(640, 480)
  
  val image = {
    val img = new BufferedImage(xPixels, yPixels, TYPE_4BYTE_ABGR)
    
    // if received as msg, draw original onto authoritative copy
    if(!tsResp.imageDataB64.isEmpty)
    {
      val imgBytes = Base64.decodeBase64(tsResp.imageDataB64)
      val receivedImg = ImageIO.read(new ByteArrayInputStream(imgBytes))
      
      img.getGraphics.drawImage(receivedImg, 0, 0, null)
    }
    
    img   
  }
  
  val selector = new TilesetTileSelector(image,
    (newSel) => println(newSel)) 
  
  add(selector, new Constraints {
    gridx = 0
    gridy = 0
  }) 
  
}
*/
