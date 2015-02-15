package rpgboss.editor

import scala.swing._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.imageset.selector._
import rpgboss.editor.misc._
import scalaswingcontrib.event._
import scalaswingcontrib.tree._
import scalaswingcontrib.tree.Tree._
import scala.swing.event._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.MapPropertiesDialog
import java.awt.event.MouseEvent
import java.awt.datatransfer.DataFlavor
import javax.swing.DropMode
import javax.swing.TransferHandler
import javax.swing.JTree
import javax.swing.JComponent
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.StringSelection
import javax.activation.DataHandler
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import rpgboss.editor.Internationalized._
import rpgboss.editor.util.MouseUtil

class ProjectPanelMapSelector(sm: StateMaster, projPanel: ProjectPanel)
  extends MapSelector(sm) {
  /**
   * Preserves ordering based on map name.
   */
  def getDropIdx(newMapName: String, siblings: Seq[Node]) = {
    val largerMapIdx =
      siblings.indexWhere(_.mapName >= newMapName)

    if (largerMapIdx == -1)
      siblings.length
    else
      largerMapIdx
  }

  /*
   * Enable dragging and set drag handler.
   */
  tree.dragEnabled = true
  tree.peer.setDropMode(DropMode.ON)
  tree.peer.setTransferHandler(new javax.swing.TransferHandler {
    override def canImport(
      support: TransferHandler.TransferSupport): Boolean = {
      if (!support.isDataFlavorSupported(DataFlavor.stringFlavor) ||
          !support.isDrop()) {
        return false
      }

      if (support.getComponent() != tree.peer) {
        return false
      }

      val dropLocation =
        support.getDropLocation().asInstanceOf[JTree.DropLocation]

      return dropLocation.getPath() != null
    }

    override def getSourceActions(c: JComponent): Int = TransferHandler.MOVE

    override def createTransferable(c: JComponent): Transferable = {
      tree.selection.paths.headOption.map { path =>
        if (path.last.mapName.isEmpty())
          return null

        return new StringSelection(path.last.mapName)
      }

      return null
    }

    override def importData(
      support: TransferHandler.TransferSupport): Boolean = {
      if (!canImport(support)) {
        println("Can't import")
        return false;
      }

      val dropLocation =
        support.getDropLocation().asInstanceOf[JTree.DropLocation]

      val dropPathJava = dropLocation.getPath()
      val dropPath = tree.treePathToPath(dropPathJava)
      val dropNode = dropPath.last
      val dropMapName = dropNode.mapName

      val transferable = support.getTransferable();

      val sourceMapName = transferable.getTransferData(
        DataFlavor.stringFlavor).toString()
      val sourceMap = sm.getMap(sourceMapName).get
      assert(allNodes.contains(sourceMapName))
      val sourceNode = allNodes.get(sourceMapName).get
      val sourceOldPath = sourceNode.getPath()

      val canDrop =
        // Don't allow drop on itself
        sourceMapName != dropMapName &&
        // Don't allow drop on existing parent
        sourceMap.metadata.parent != dropMapName &&
        // Don't allow drop on any of its descendants
        sourceOldPath != dropPath.take(sourceOldPath.length)

      // Don't allow dropping on itself or its existing parent.
      if (canDrop) {
        // Modify the actual metadata
        sourceMap.metadata.parent = dropNode.mapName
        sm.setMap(sourceMapName, sourceMap, markDirty = true)
        val sourceNewPath = sourceNode.getPath()

        // Update the tree structure to reflect it.
        def recursiveCopy(sourcePath: Path[Node],
                          destParentPath: Path[Node]): Unit = {
          val sourceNode = sourcePath.last
          val sourceWasExpanded = tree.isExpanded(sourcePath)
          tree.model.insertUnder(
            destParentPath,
            sourceNode,
            getDropIdx(sourceNode.mapName,
                       tree.model.getChildrenOf(destParentPath)))

          val destPath = destParentPath :+ sourceNode
          for (childNode <- tree.model.getChildrenOf(sourcePath)) {
            val childPath = sourcePath :+ childNode
            recursiveCopy(childPath, destPath)
          }

          if (sourceWasExpanded)
            tree.expandPath(destPath)
          else
            tree.collapsePath(destPath)
        }

        recursiveCopy(sourceOldPath, sourceNewPath.dropRight(1))
        tree.model.remove(sourceOldPath)

        highlightWithoutEvent(sourceNode)
      }

      return true;
    }
  })

  /*
   * Popup actions
   */
  def popupMenuFor(node: Node) = {
    new RpgPopupMenu {
      if (node != projectRoot) {
        contents += new MenuItem(Action(getMessage("Map_Properties") + "...") {
          val origMap = sm.getMap(node.mapName).get
          val origMapData = sm.getMapData(node.mapName)

          val d = new MapPropertiesDialog(
            projPanel.mainP.topWin,
            sm,
            "New Map",
            origMap,
            origMapData,
            (updatedMap, updatedMapData) => {
              sm.setMap(origMap.name, updatedMap)
              sm.setMapData(origMap.name, updatedMapData)

              // Update tree view in case title changed.
              val oldNode = allNodes.apply(updatedMap.name)
              val newNode = Node.apply(updatedMap)
              tree.model.update(oldNode.getPath(), newNode)

              // Select map again to refresh the map view and tileset selector
              projPanel.selectMap(Some(updatedMap))
            })
          d.open()
        })
        contents += new MenuItem(Action(getMessage("Delete")) {
          val msg =
            "Are you sure you want to delete map '%s'?".format(node.mapName)
          val answer = Dialog.showConfirmation(this, msg, "Delete")
          if (answer == Dialog.Result.Yes) {
            removeNode(node)
            sm.removeMap(node.mapName)

            val map = sm.getMap(node.mapName).get
            allNodes
              .get(map.metadata.parent)
              .map(selectNode(_))
              .getOrElse(selectNode(projectRoot))
          }
        })
        contents += new MenuItem(Action(getMessage("Duplicate_Map") + "...") {
          val answer = Dialog.showConfirmation(this, "This function is not implemented yet.", "Notice")
          
        })
        contents += new Separator
      }
      contents += new MenuItem(Action(getMessage("New_Map") + "...") {
        // Generate a new map with an incremented map id name
        val newMap = RpgMap.defaultInstance(
          sm.getProj,
          RpgMap.generateName(sm.getProj.data.lastCreatedMapId + 1))

        newMap.metadata.parent = node.mapName

        val d = new MapPropertiesDialog(
          projPanel.mainP.topWin,
          sm,
          "New Map",
          newMap,
          RpgMap.defaultMapData,
          (newMap, newMapData) => {
            val p = sm.getProj
            sm.setProjData(p.data.copy(
              lastCreatedMapId = p.data.lastCreatedMapId + 1))

            val parentNode = allNodes.get(newMap.metadata.parent).get
            val newNode = Node(newMap)
            val parentPath = parentNode.getPath()
            val parentsChildren = tree.model.getChildrenOf(parentPath)

            tree.model.insertUnder(parentPath, newNode,
              getDropIdx(newMap.name, parentsChildren))

            // Add to the state master. Don't actually write it ourselves
            sm.addMap(newMap, Some(newMapData), Dirtiness.Dirty)

            highlightWithoutEvent(newNode)

            // select the new map
            projPanel.selectMap(Some(newMap))
          })
        d.open()
      })
    }
  }

  override def onSelectMap(map: Option[RpgMap]) = {
    projPanel.selectMap(map)
  }

  listenTo(tree.mouse.clicks)

  reactions += {
    case e: MouseClicked if e.source == tree => {
      val (x0, y0) = (e.point.getX().toInt, e.point.getY().toInt)

      if (MouseUtil.isRightClick(e)) {
        val clickRow = tree.getRowForLocation(x0, y0)

        // Temporarily disable selection events while popup in action
        deafTo(tree.selection)
        deafTo(tree.mouse.clicks)

        // The previously selected path
        val origRow = tree.selection.rows.headOption

        if (clickRow != -1)
          tree.selectRows(clickRow)

        val clickNode =
          if (clickRow == -1)
            projectRoot
          else
            tree.selection.paths.head.last

        val menu = popupMenuFor(clickNode)
        menu.showWithCallback(tree, x0, y0, onHide = () => {
          origRow.map(p => tree.selectRows(p))

          // Renable all eventns
          listenTo(tree.selection)
          listenTo(tree.mouse.clicks)
        })
      }
    }
  }
}