package rpgboss.editor.imageset.metadata

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.misc.TileUtils
import scala.swing.event._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster
import rpgboss.editor.imageset.selector._
import javax.imageio.ImageIO
import java.awt.geom.Line2D
import java.awt.AlphaComposite

/**
 * @param autotiles   If true, this  list item denotes autotiles. Other two
 *                    params are then unimportant.
 *
 * @param text        What to display in the ListView
 *
 * @param tilesetIdx  Which tileset index it's associated with. Only used if
 *                    autotiles == false
 */
case class TilesetListItem(autotiles: Boolean, text: String, tilesetIdx: Int) {
  override def toString() = text
}

class TilesetsMetadataPanel(sm: StateMaster)
  extends BoxPanel(Orientation.Horizontal) {

  // A mutable array that we update as we modify things
  val autotiles =
    Autotile.list(sm.getProj).map(Autotile.readFromDisk(sm.getProj, _))

  val dirtyAutotileIdxs = collection.mutable.Set[Int]()

  // We don't modify this array directly, but modify the contents
  val tilesets =
    Tileset.list(sm.getProj).map(Tileset.readFromDisk(sm.getProj, _))

  val dirtyTilesetIdxs = collection.mutable.Set[Int]()

  // Two variables
  var autotilesSelected = true
  var tilesetIdx = -1

  var metadataMode = MetadataMode.default

  /**
   * Returns a tuple of metadata i.e. Some((blockedDirs)) if tile exists
   * tilesetIdx is always going to be valid when this is called
   * Return None if (xTile, yTile) is invalid
   */
  def getTileMeta(x: Int, y: Int) = {
    if (autotilesSelected) {
      val idx = x
      if (idx < autotiles.length) {
        val m = autotiles(idx).metadata
        Some(TileMetadata(m.blockedDirs, m.height, m.normalizedVehicleDirs))
      } else None
    } else {
      val tileset = tilesets(tilesetIdx)
      val blockedDir = tileset.metadata.blockedDirsAry(y)(x)
      val height = tileset.metadata.heightAry(y)(x)
      Some(TileMetadata(blockedDir, height,
          AutotileMetadata.defaultVehicleDirs))
    }
  }

  def inBounds(xTile: Int, yTile: Int): Boolean = {
    if (autotilesSelected) {
      true
    } else {
      val tileset = tilesets(tilesetIdx)
      tileset.inBounds(xTile, yTile)
    }
  }

  /**
   * User clicks this tile. Not required to do anything.
   * tilesetIdx is always going to be valid when this is called
   */
  def updateTileMeta(x: Int, y: Int, newMetadata: TileMetadata) = {
    if (autotilesSelected) {
      val (newBlockedDirs) = newMetadata
      val idx = x
      val autotile = autotiles(idx)
      val newAutotileMetadata = autotile.metadata.copy(
        blockedDirs = newMetadata.blockedDirs,
        height = newMetadata.height,
        vehicleDirs = newMetadata.vehicleDirs)
      autotiles.update(idx, autotile.copy(metadata = newAutotileMetadata))
      dirtyAutotileIdxs.add(idx)
    } else {
      val tileset = tilesets(tilesetIdx)
      val m = tileset.metadata
      m.blockedDirsAry(y).update(x, newMetadata.blockedDirs)
      m.heightAry(y).update(x, newMetadata.height)

      dirtyTilesetIdxs.add(tilesetIdx)
    }
  }

  val tilesetListView = new ListView(
    Array(TilesetListItem(true, "<html><b>*** Autotiles</b></html>", -1)) ++
      tilesets.zipWithIndex.map {
        case (ts, i) => TilesetListItem(false, ts.name, i)
      })
  val metadataPanelContainer = new BoxPanel(Orientation.Vertical) {
    preferredSize = new Dimension(8 * 32 + 12, 500)
  }

  // Guaranteed to be called with a valid index
  def updateTilesetSelection(selectAutotiles: Boolean, idx: Int) = {
    // Clear old item
    metadataPanelContainer.contents.clear()

    if (selectAutotiles) {
      def srcImg = TileUtils.getAutotileCollageImg(autotiles)
      metadataPanelContainer.contents +=
        new TileMetadataPanel(srcImg, this, true)
      autotilesSelected = true
    } else {
      val t = tilesets(idx)
      metadataPanelContainer.contents +=
        new TileMetadataPanel(t.img, this, false)
      tilesetIdx = idx
      autotilesSelected = false
    }

    metadataPanelContainer.revalidate()
  }

  def save() = {
    // TODO: Perhaps we should only save the actually modified ones...
    (tilesets ++ autotiles).map(_.writeMetadata())
  }

  contents += new DesignGridPanel {
    row.grid().add(leftLabel("Tilesets:"))
    row.grid().add(tilesetListView)
  }

  contents += metadataPanelContainer

  contents += new DesignGridPanel {
    val btns = enumButtons(MetadataMode)(
      metadataMode,
      newMode => {
        metadataMode = newMode
        metadataPanelContainer.repaint()
      },
      Nil)

    new ButtonGroup(btns: _*)

    row().grid().add(leftLabel("Edit mode:"))
    btns.foreach { btn =>
      row().grid().add(btn)
    }
  }

  listenTo(tilesetListView.selection)

  reactions += {
    case ListSelectionChanged(`tilesetListView`, _, _) =>
      val item = tilesetListView.selection.items.head
      if (item.autotiles) {
        updateTilesetSelection(true, -1)
      } else {
        updateTilesetSelection(false, item.tilesetIdx)
      }
  }

  // Init selection
  tilesetListView.selectIndices(0)
}
