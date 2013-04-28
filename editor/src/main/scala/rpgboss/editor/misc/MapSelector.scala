package rpgboss.editor.misc

import rpgboss.editor._
import scala.swing._
import scala.swing.event._
import scalaswingcontrib.tree._
import scalaswingcontrib.event._
import scalaswingcontrib.tree.Tree._
import rpgboss.model._
import rpgboss.model.resource._

class MapSelector(sm: StateMaster)
  extends ScrollPane {
  val allNodes = collection.mutable.Map[String, Node]()

  /* 
   * TODO: Currently assume no map will be "orphaned". Add code to
   * handle orphans later
   */
  class Node(
    val displayName: String,
    val mapName: String,
    ancestors: Path[Node]) {
    def children: Seq[Node] =
      sm.getMapMetas
        .filter(_.metadata.parent == mapName)
        .map(Node.apply(_, path))

    // XXX: Should the allNodes addition really be in the constructor? Shady.
    allNodes.put(mapName, this)

    val path = ancestors :+ this
  }

  object Node {
    def apply(m: RpgMap, path: Path[Node]) =
      new Node(m.displayName, m.name, path)
  }

  def removeNode(n: Node) = {
    allNodes.remove(n.mapName)
    tree.model.remove(n.path)
  }

  val projectRoot = new Node(sm.getProj.data.title, "", Path())

  minimumSize = new Dimension(8 * 32, 200)
  preferredSize = new Dimension(8 * 32, 200)

  val tree = new Tree[Node] {
    model = InternalTreeModel(projectRoot)(_.children)

    renderer = Renderer({
      case n: Node => n.displayName
    })
  }

  // Necessary to get the reaction event working :(
  val treeAny = tree.asInstanceOf[Tree[Any]]
  tree.expandAll()
  tree.collapseAll()

  tree.expandPath(Path(projectRoot))

  def getNode(mapName: String) = {
    allNodes.get(mapName).map { node =>
      node
    } getOrElse {
      throw new RuntimeException("Map: %s not found.".format(mapName))
    }
  }

  def highlightWithoutEvent(node: Node) = {
    deafTo(tree.selection)
    selectNode(node)
    listenTo(tree.selection)
  }

  def selectNode(node: Node) = {
    tree.expandPath(node.path)
    tree.selectPaths(node.path)
  }

  contents = tree

  listenTo(tree.selection)

  def onSelectMap(map: Option[RpgMap]) = {}

  reactions += {
    case TreePathSelected(`treeAny`, newPathListAny, _, _, _) => {
      if (!newPathListAny.isEmpty) {
        val newPath = newPathListAny.last.asInstanceOf[Path[Node]]

        val mapName = newPath.last.mapName

        if (mapName.isEmpty()) {
          onSelectMap(None)
        } else {
          val map = sm.getMap(mapName)

          onSelectMap(Some(map))
        }
      }
    }
  }
}
