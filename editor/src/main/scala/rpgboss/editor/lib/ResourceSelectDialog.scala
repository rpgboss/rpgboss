package rpgboss.editor.lib

import scala.swing._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.model._
import rpgboss.model.resource._
import com.weiglewilczek.slf4s.Logging
import java.awt.Dimension
import rpgboss.editor.StateMaster
import rpgboss.editor.lib.ImagePanel
import rpgboss.editor.dialog.StdDialog
import scala.Array.canBuildFrom
import javax.swing.border.LineBorder

abstract class ResourceSelectDialog[SpecType, T, MT](
    owner: Window, 
    sm: StateMaster,
    initialSelectionOpt: Option[SpecType],
    onSuccess: (Option[SpecType]) => Any,
    allowNone: Boolean,
    metaResource: MetaResource[T, MT])
  extends StdDialog(owner, "Select " + metaResource.rcType.capitalize) 
  with Logging 
{
  
  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType
  
  def rightPaneFor(
      selection: SpecType,
      updateSelectionF: SpecType => Unit): Component
  
  val allResources = metaResource.list(sm.getProj)
  
  var curSelection : Option[SpecType] = None
  
  def okFunc(): Unit = { 
    onSuccess(curSelection)
    close()
  }
  
  def listContents = 
    (if(allowNone) List(None) else Nil) ++ allResources.map(Some(_))
  
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
  
  contents = new DesignGridPanel {
    row().grid().add(new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        row.grid().add(leftLabel("Select " + metaResource.rcType + ":"))
        row.grid().add(rcList)
      }
      contents += rightPaneContainer
    })
    addButtons(cancelBtn, okBtn)
  }
  
  listenTo(rcList.selection)
  reactions += {
    case ListSelectionChanged(`rcList`, _, _) =>
      val newSpecOpt = 
        rcList.selection.items.head.map(newRcNameToSpec(_, curSelection))
      
      updateSelection(newSpecOpt)
  }
  
  // Initialize the selection, but first check to make sure it's valid
  initialSelectionOpt.map { initSel =>
    val rcName = specToResourceName(initSel)
    if(allResources.contains(rcName)) {
      val idx = rcList.listData.indexOf(Some(rcName))
      rcList.selectIndices(idx)
    }else if(allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
    }
  } getOrElse {
    if(allowNone) {
      val idx = rcList.listData.indexOf(None)
      rcList.selectIndices(idx)
    }
  }
}