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

case class TileMetadata(blockedDirs: Byte)

trait TileMetadataPanelOwner {
  /**
   * Returns a tuple of metadata i.e. Some((blockedDirs)) if tile exists
   * Return None if (xTile, yTile) is invalid
   */
  def getTileMeta(xTile: Int, yTile: Int): Option[TileMetadata]
  
  /**
   * User clicks this tile. Not required to do anything. 
   */
  def updateTileMeta(xTile: Int, yTile: Int, newMetadata: TileMetadata): Unit
}

class TileMetadataPanel(srcImg: BufferedImage, owner: TileMetadataPanelOwner) 
  extends BoxPanel(Orientation.Horizontal) {
  
  // Returns the new tile metadata as a result of the click
  def updatedMetadata(xTile: Int, yTile: Int): Option[TileMetadata] = {
    owner.getTileMeta(xTile, yTile) map { curMetadata => 
      import Constants.DirectionMasks._
      
      val newMetadata = 
        if(tileClicker.mode == Passability) {
          val newBlockedDirs = 
            if(allBlocked(curMetadata.blockedDirs)) {
              NONE
            } else {
              ALLCARDINAL
            }
          curMetadata.copy(blockedDirs = newBlockedDirs.toByte)
        } else {
          curMetadata
        }
      
      newMetadata
    }
  }
  
  val tileClicker = new ImageTileSelector(
      srcImg = srcImg, 
      selectTileF = { tXYArray =>
        val xTile = tXYArray.head.head._1
        val yTile = tXYArray.head.head._2
        updatedMetadata(xTile, yTile) map { newMetadata =>
          owner.updateTileMeta(xTile, yTile, newMetadata)
        }
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
      
      // All these values are in selector space
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
      
      /*
       * Since the coordinates are natively in the selector space, we must
       * covert to the srcImg space (tileset space) before we do ops on it
       */
      for(yTileSelSpace <- minYTile to maxYTile;
          xTileSelSpace <- minXTile to maxXTile; 
          val (xTile, yTile) = toTilesetSpace(xTileSelSpace, yTileSelSpace); 
          metadata <- owner.getTileMeta(xTile, yTile)) {
          
        if(allPassable(metadata.blockedDirs))
          draw22Icon(iconPass, xTileSelSpace, yTileSelSpace)
        else if(allBlocked(metadata.blockedDirs))
          draw22Icon(iconStop, xTileSelSpace, yTileSelSpace)
        else
          None
      }
    }
  }
    
  
  contents += tileClicker
  
  contents += new BoxPanel(Orientation.Vertical) {
    
  }
}
