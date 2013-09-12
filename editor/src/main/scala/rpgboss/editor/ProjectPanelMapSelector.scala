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

class ProjectPanelMapSelector(sm: StateMaster, projPanel: ProjectPanel)
  extends MapSelector(sm) {
  /*
   * Popup actions
   */
  def popupMenuFor(node: Node) = {
    new PopupMenu {
      if (node != projectRoot) {
        contents += new MenuItem(Action("Map Properties...") {
          val origMap = sm.getMap(node.mapName)
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

              val origNode = allNodes.get(origMap.name).get
              val newParentNode = allNodes.get(updatedMap.metadata.parent).get

              val newIdx = if (newParentNode.path == origNode.path.init) {
                // Same parent as before... Will find among siblings
                val siblings = tree.model.getChildrenOf(origNode.path.init)
                siblings.indexOf(origNode)
              } else {
                // Insert at end of list
                tree.model.getChildrenOf(newParentNode.path).length
              }
              
              removeNode(origNode)

              val newNode = Node(updatedMap, newParentNode.path)
              tree.model.insertUnder(newParentNode.path, newNode, newIdx)
              highlightWithoutEvent(newNode)

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
            
            val map = sm.getMap(node.mapName)
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
            val newNode = Node(newMap, parentNode.path)
            val idx = tree.model.getChildrenOf(parentNode.path).length
            tree.model.insertUnder(parentNode.path, newNode, idx)
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