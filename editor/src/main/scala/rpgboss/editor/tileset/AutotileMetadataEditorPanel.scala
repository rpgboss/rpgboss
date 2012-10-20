package rpgboss.editor.tileset

import scala.swing._
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

class AutotileMetadataEditorPanel(sm: StateMaster) 
  extends BoxPanel(Orientation.Horizontal) {
  
  // A mutable array that we update as we modify things
  val autotiles = 
    Autotile.list(sm.getProj).map(Autotile.readFromDisk(sm.getProj, _))
  
  val dirtyIdxs = collection.mutable.Set[Int]()
    
  val collageImage = TileUtils.getAutotileCollageImg(autotiles)
  
  // idx is guaranteed to be valid here.
  def clickAutotile(idx: Int): Unit = {
    val autotile = autotiles(idx)
    
    import Constants.DirectionMasks._
    import tileClicker.ModeEnum._
    
    val newMetadata = 
      if(tileClicker.mode == Passability) {
        val newBlockedDirs = 
          if(autotile.allBlocked) {
            NONE
          } else {
            ALLCARDINAL
          }
        autotile.metadata.copy(blockedDirs = newBlockedDirs.toByte)
      } else {
        autotile.metadata
      }
    
    val newAutotile = autotile.copy(metadata=newMetadata)
    
    autotiles.update(idx, newAutotile)
    
    dirtyIdxs.add(idx)
  }
  
  val tileClicker = new ImageTileSelector(
      srcImg = collageImage, 
      selectTileF = { tXYArray =>
        // Just xTile, as the collageImage just puts it in a huge row
        val autotileIdx = tXYArray.head.head._1 
        clickAutotile(autotileIdx)
      },
      allowMultiselect = false,
      drawSelectionSq = false) {
    
    val cl = getClass.getClassLoader
    
    def loadIcon(path: String) = ImageIO.read(cl.getResourceAsStream(path))
    
    val iconPass = loadIcon("oxygen/22x22/actions/dialog-ok-apply_mod.png")
    val iconStop = loadIcon("oxygen/22x22/actions/edit-delete_mod.png")
    
    object ModeEnum extends Enumeration {
      type Mode = Value
      val Passability = Value
    }
    import ModeEnum._
    
    var mode: Mode = Passability
    
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
      
      for(xTile <- minXTile to maxXTile; yTile <- minYTile to maxYTile) {
        val autotileIdx = yTile*xTilesVisible + xTile
        if(autotileIdx < autotiles.length) {
          val autotile = autotiles(autotileIdx)
          
          if(autotile.allPassable)
            draw22Icon(iconPass, xTile, yTile)
          else if(autotile.allBlocked)
            draw22Icon(iconStop, xTile, yTile)
          else
            None
        }
      }
    }
  }
    
  
  contents += tileClicker
  
  contents += new BoxPanel(Orientation.Vertical) {
    
  }
}
