package rpgboss.editor.resourceselector

import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import scala.swing.event.ListSelectionChanged
import com.badlogic.gdx.utils.Disposable

trait ResourceRightPane extends Component {
  def dispose() = {}
}

// TODO: Refactor this class. It's used for both choosing a resource, in
// addition to specifying usage of the resource, i.e. volume and pitch for
// a sound effect. It's becoming pretty incomprehensible.
abstract class ResourceSelectPanel[SpecType, T, MT](
  sm: StateMaster,
  initialSelectionOpt: Option[SpecType],
  allowNone: Boolean,
  val metaResource: MetaResource[T, MT])
  extends DesignGridPanel with LazyLogging with Disposable {

  layout.margins(0)

  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType

  var currentRightPane: Option[ResourceRightPane] = None

  def dispose() = currentRightPane.map(_.dispose())

  def rightPaneFor(
    selection: SpecType,
    updateSelectionF: SpecType => Unit): ResourceRightPane =
      new BoxPanel(Orientation.Vertical) with ResourceRightPane

  val allResources = metaResource.list(sm.getProj)

  var curSelection: Option[SpecType] = None

  def listContents =
    (if (allowNone) List(None) else Nil) ++ allResources.map(Some(_))

  val rcList = new ListView(listContents) {
    renderer = ListView.Renderer({ opt =>
      opt getOrElse "<None>"
    })
  }

  val rightPaneContainer = new BoxPanel(Orientation.Vertical)

  // Must call with valid arguments.
  // Call this only when updating both the spriteset and the spriteIndex
  def updateSelection(specOpt: Option[SpecType]) = {

    logger.info("Selected a different " + metaResource.rcType)

    curSelection = specOpt

    // Update the sprite index selection panel
    currentRightPane.map(_.dispose())
    rightPaneContainer.contents.clear()
    curSelection.map { spriteSpec =>
      // TODO: Do we really want to create a whole new right pane every time
      // we modify some aspect of the current selection? i.e. volume on a sfx.
      val newPane = rightPaneFor(spriteSpec, x => curSelection = Some(x))
      rightPaneContainer.contents += newPane
      currentRightPane = Some(newPane)
    } getOrElse {
      rightPaneContainer.contents += new Label("None")
      currentRightPane = None
    }

    rightPaneContainer.revalidate()
    rightPaneContainer.repaint()
  }

  // Initialize the selection, but first check to make sure it's valid
  initialSelectionOpt.map { initSel =>
    val rcName = specToResourceName(initSel)
    if (allResources.contains(rcName)) {
      curSelection = initialSelectionOpt
      val idx = rcList.listData.indexOf(Some(rcName))
      rcList.selectIndices(idx)
      updateSelection(curSelection)
    } else if (allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
      updateSelection(None)
    }
  } getOrElse {
    if (allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
      updateSelection(None)
    } else if (!rcList.listData.isEmpty) {
      rcList.selectIndices(0)
      updateSelection(
        rcList.selection.items.head.map(newRcNameToSpec(_, curSelection)))
    }
  }

  row().grid().add(new BoxPanel(Orientation.Horizontal) {
    contents += new DesignGridPanel {
      maximumSize = new Dimension(250, 5000)
      preferredSize = new Dimension(250, 500)
      minimumSize = new Dimension(250, 250)

      row.grid().add(new Label("Select " + metaResource.rcType + ":"))
      row.grid().add(new ScrollPane {
        contents = rcList
      })
    }
    contents += rightPaneContainer
  })

  listenTo(rcList.selection)
  reactions += {
    case ListSelectionChanged(`rcList`, _, false) =>
      val firstSelection = rcList.selection.items.head
      val newSpecOpt = firstSelection.map(newRcNameToSpec(_, curSelection))

      updateSelection(newSpecOpt)
  }
}