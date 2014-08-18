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

class ProjectPanelMapSelector(sm: StateMaster, projPanel: ProjectPanel)
  extends MapSelector(sm) {
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

      val dropPath = dropLocation.getPath()
      val dropPathScala = tree.treePathToPath(dropPath)
      val dropNode = dropPathScala.last

      val transferable = support.getTransferable();

      val sourceMapName = transferable.getTransferData(
        DataFlavor.stringFlavor).toString()

      val origMap = sm.getMap(sourceMapName).get
      // Don't allow dropping on itself or its existing parent.
      if (sourceMapName != dropNode.mapName &&
          origMap.metadata.parent != dropNode.mapName) {
        assert(allNodes.contains(sourceMapName))

        val node = allNodes.get(sourceMapName).get

        tree.model.remove(node.getPath())

        assert(allNodes.contains(dropNode.mapName))
        tree.model.insertUnder(dropPathScala, node, 0)

        origMap.metadata.parent = dropNode.mapName
        sm.setMap(sourceMapName, origMap, markDirty = true)

        highlightWithoutEvent(node)
      }

      return true;
    }
  })

  def recursiveMoveNodes(sourceNode: Node, newParent: Node) = {

  }

  /*
   * Popup actions
   */
  def popupMenuFor(node: Node) = {
    new RpgPopupMenu {
      if (node != projectRoot) {
        contents += new MenuItem(Action("Map Properties...") {
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

              // Select map again to refresh the map view and tileset selector
              projPanel.selectMap(Some(updatedMap))
            })
          d.open()
        })
        contents += new MenuItem(Action("Delete") {
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
        contents += new Separator
      }
      contents += new MenuItem(Action("New Map...") {
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
            val idx = tree.model.getChildrenOf(parentPath).length
            tree.model.insertUnder(parentPath, newNode, idx)
            highlightWithoutEvent(newNode)

            // Add to the state master. Don't actually write it ourselves
            sm.addMap(newMap, Some(newMapData), Dirtiness.Dirty)

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

      if (e.peer.getButton() == MouseEvent.BUTTON3) {
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