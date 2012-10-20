package rpgboss.editor.tileset.metadata

import scala.swing._

import rpgboss.editor.tileset._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.editor.lib._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster
import javax.imageio.ImageIO
import java.awt.geom.Line2D
import java.awt.AlphaComposite

object MetadataModeEnum extends Enumeration {
  type MetadataMode = Value
  val Passability = Value
}
import MetadataModeEnum._

trait TileMetadataPanelOwner {
  // Returns a tuple of metadata i.e. Some((blockedDirs)) if tile exists
  def getTileMeta(xTile: Int, yTile: Int): Option[(Byte)]
  def clickTile(xTile: Int, yTile: Int): Unit
}

class TileMetadataPanel(srcImg: BufferedImage, owner: TileMetadataPanelOwner) 
  extends BoxPanel(Orientation.Horizontal) {
  
  // Returns the new tile metadata as a result of the click
  def getNewTileMetadataFromClick(xTile: Int, yTile: Int): Option[(Byte)] = {
    owner.getTileMeta(xTile, yTile) map { blockedDirs => 
      import Constants.DirectionMasks._
      
      val newMetadataTuple: (Byte) = 
        if(tileClicker.mode == Passability) {
          val newBlockedDirs = 
            if(allBlocked(blockedDirs)) {
              NONE
            } else {
              ALLCARDINAL
            }
          (newBlockedDirs.toByte)
        } else {
          (blockedDirs)
        }
      
      newMetadataTuple
    }
  }
  
  val tileClicker = new ImageTileSelector(
      srcImg = srcImg, 
      selectTileF = { tXYArray =>
        owner.clickTile(tXYArray.head.head._1, tXYArray.head.head._2)
      },
      allowMultiselect = false,
      drawSelectionSq = false) {
    
    val cl = getClass.getClassLoader
    
    def loadIcon(path: String) = ImageIO.read(cl.getResourceAsStream(path))
    
    val iconPass = loadIcon("oxygen/22x22/actions/dialog-ok-apply_mod.png")
    val iconStop = loadIcon("oxygen/22x22/actions/edit-delete_mod.png")
    
    var mode: MetadataMode = Passability
    
    override def canvasPanelPaintComponent(g: Graphics2D) = {
      super.canvasPanelPaintComponent(g)
      
      val (minX, minY, maxX, maxY, minXTile, minYTile, maxXTile, maxYTile) =
          TileUtils.getTileBounds(
              g.getClipBounds(), tilesizeX, tilesizeY, 
              xTilesVisible, imageSlices*yTilesInSlice)
      
      // Draw grid
      TileUtils.drawGrid(
          g, tilesizeX, tilesizeY, minXTile, minYTile, maxXTile, maxYTile)
      
      // Draw passabilty
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f))
      
      import Constants.DirectionMasks._
      
      def draw22Icon(icon: BufferedImage, xTile: Int, yTile: Int) = {
        val x1 = xTile*tilesizeX + (tilesizeX-22)/2
        val y1 = yTile*tilesizeY + (tilesizeY-22)/2
        g.drawImage(icon, x1, y1, null)
      }
      
      for(xTile <- minXTile to maxXTile; 
          yTile <- minYTile to maxYTile;
          blockedDirs <- owner.getTileMeta(xTile, yTile)) {
          
        if(allPassable(blockedDirs))
          draw22Icon(iconPass, xTile, yTile)
        else if(allBlocked(blockedDirs))
          draw22Icon(iconStop, xTile, yTile)
        else
          None
      }
    }
  }
    
  
  contents += tileClicker
  
  contents += new BoxPanel(Orientation.Vertical) {
    
  }
}
