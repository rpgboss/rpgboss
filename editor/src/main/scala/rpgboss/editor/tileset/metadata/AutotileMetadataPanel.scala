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
  
  def getTileMeta(xTile: Int, yTile: Int) = {
    val idx = xTile
    if(idx < autotiles.length) {
      Some(TileMetadata(autotiles(idx).metadata.blockedDirs))
    } else None
  }
  
  def updateTileMeta(xTile: Int, yTile: Int, newMetadata: TileMetadata) = {
    val (newBlockedDirs) = newMetadata      
    val idx = xTile
    val autotile = autotiles(idx)
    val newAutotileMetadata = autotile.metadata.copy(
        blockedDirs = newMetadata.blockedDirs)
    autotiles.update(idx, autotile.copy(metadata = newAutotileMetadata))
    dirtyIdxs.add(idx)
  }
  
  def save() = {
    for(i <- dirtyIdxs) {
      autotiles(i).writeMetadata()
    }
  }
  
  val tileMetadataPanel = new TileMetadataPanel(srcImg, this)
  contents += tileMetadataPanel
}
