package rpgboss.editor.tileset.metadata

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

class AutotileMetadataPanel(sm: StateMaster) 
  extends BoxPanel(Orientation.Horizontal) 
  with TileMetadataPanelOwner {
  
  // A mutable array that we update as we modify things
  val autotiles = 
    Autotile.list(sm.getProj).map(Autotile.readFromDisk(sm.getProj, _))
  
  val dirtyIdxs = collection.mutable.Set[Int]()
  
  def srcImg = TileUtils.getAutotileCollageImg(autotiles)
  
  def getTileMeta(xTile: Int, yTile: Int): Option[(Byte)] = {
    val idx = xTile+yTile*tileClicker.xTilesVisible
    if(idx < autotiles.length) {
      Some((autotiles(idx).metadata.blockedDirs))
    } else None
  }
  
  def clickTile(xTile: Int, yTile: Int): Unit = {
    tileMetadataPanel.getNewTileMetadataFromClick(xTile, yTile) map { 
      newMetadata =>
        val (newBlockedDirs) = newMetadata      
        val idx = xTile+yTile*tileClicker.xTilesVisible
        val autotile = autotiles(idx)
        val newAutotileMetadata = autotile.metadata.copy(
            blockedDirs = newBlockedDirs)
        autotiles.update(idx, autotile.copy(metadata = newAutotileMetadata))
        dirtyIdxs.add(idx)
    }
  }
  
  val tileMetadataPanel = new TileMetadataPanel(srcImg, this)
  def tileClicker = tileMetadataPanel.tileClicker
  
  contents += tileMetadataPanel
}
