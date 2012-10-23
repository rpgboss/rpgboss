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

case class TilesetListItem(tileset: Tileset) {
  override def toString() = tileset.name
}

class TilesetsMetadataPanel(sm: StateMaster) 
  extends BoxPanel(Orientation.Horizontal) 
  with TileMetadataPanelOwner {
  
  // We don't modify this array directly, but modify the contents
  val tilesets = 
    Tileset.list(sm.getProj).map(Tileset.readFromDisk(sm.getProj, _))
  
  val dirtyTilesetIdxs = collection.mutable.Set[Int]()
  
  // tilesetIdx is always going to be valid when this is called
  def getTileMeta(xTile: Int, yTile: Int) = {
    val tileset = tilesets(tilesetIdx)
    val blockedDir = tileset.metadata.blockedDirsAry(yTile)(xTile)
    Some(TileMetadata(blockedDir.toByte))
  }
  
  // tilesetIdx is always going to be valid when this is called
  def updateTileMeta(xTile: Int, yTile: Int, newMetadata: TileMetadata) = {      
    val tileset = tilesets(tilesetIdx)
    tileset.metadata
      .blockedDirsAry(yTile).update(xTile, newMetadata.blockedDirs)
    
    dirtyTilesetIdxs.add(tilesetIdx)
  }
  
  val tilesetListView = new ListView(tilesets.map(TilesetListItem))
  val metadataPanelContainer = new BoxPanel(Orientation.Vertical) {
    preferredSize = new Dimension(8*32+12, 500)
  }
  
  var tilesetIdx = -1
  
  // Guaranteed to be called with a valid index
  def updateTilesetSelection(idx: Int) = {
    val t = tilesets(idx)
    metadataPanelContainer.contents.clear()
    
    metadataPanelContainer.contents += new TileMetadataPanel(t.img, this)
    
    tilesetIdx = idx
    metadataPanelContainer.revalidate()
  }
  
  def save() = {
    for(i <- dirtyTilesetIdxs) {
      tilesets(i).writeMetadata()
    }
  }
  
  if(!tilesets.isEmpty) updateTilesetSelection(0)
  
  contents += new DesignGridPanel {
    row.grid().add(leftLabel("Tilesets:")).add(metadataPanelContainer)
    row.grid().add(tilesetListView).spanRow()
  } 
  
  listenTo(tilesetListView.selection)
  
  reactions += {
    case ListSelectionChanged(`tilesetListView`, _, _) =>
      updateTilesetSelection(tilesetListView.selection.indices.head)
  }
}
