package rpgboss.editor.resourceselector

import com.typesafe.scalalogging.slf4j.Logging
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import scala.swing.event.ListSelectionChanged

abstract class ResourceSelectPanel[SpecType, T, MT](
  sm: StateMaster,
  initialSelectionOpt: Option[SpecType],
  allowNone: Boolean,
  val metaResource: MetaResource[T, MT])
  extends DesignGridPanel with Logging {
  
  layout.margins(0)

  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType

  def rightPaneFor(
    selection: SpecType,
    updateSelectionF: SpecType => Unit): Component = 
      new BoxPanel(Orientation.Vertical)

  val allResources = metaResource.list(sm.getProj)

  var curSelection: Option[SpecType] = None

  def listContents =
    (if (allowNone) List(None) else Nil) ++ allResources.map(Some(_))

  val rcList = new ListView(listContents) {
    renderer = ListView.Renderer({ opt =>
      opt getOrElse "<None>"
    })
  }

  def rightPaneDim = new Dimension(384, 384)
  val rightPaneContainer = new BoxPanel(Orientation.Vertical) {
    preferredSize = rightPaneDim
    maximumSize = rightPaneDim
    minimumSize = rightPaneDim
  }

  // Must call with valid arguments.
  // Call this only when updating both the spriteset and the spriteIndex
  def updateSelection(specOpt: Option[SpecType]) = {

    logger.info("Selected a different " + metaResource.rcType)

    curSelection = specOpt

    // Update the sprite index selection panel
    curSelection.map { spriteSpec =>
      rightPaneContainer.contents.clear()
      rightPaneContainer.contents +=
        rightPaneFor(spriteSpec, x => curSelection = Some(x))

    } getOrElse {
      rightPaneContainer.contents.clear()
      rightPaneContainer.contents += new Label("None")
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
    } else if (allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
    }
  } getOrElse {
    if (allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
    }
  }
  updateSelection(curSelection)

  row().grid().add(new BoxPanel(Orientation.Horizontal) {
    contents += new DesignGridPanel {
      row.grid().add(new Label("Select " + metaResource.rcType + ":"))
      row.grid().add(rcList)
    }
    contents += rightPaneContainer
  })

  listenTo(rcList.selection)
  reactions += {
    case ListSelectionChanged(`rcList`, _, _) =>
      val newSpecOpt =
        rcList.selection.items.head.map(newRcNameToSpec(_, curSelection))

      updateSelection(newSpecOpt)
  }
}