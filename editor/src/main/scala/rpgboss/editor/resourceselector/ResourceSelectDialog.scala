package rpgboss.editor.resourceselector

import scala.swing._
import rpgboss.editor.misc.SwingUtils._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase._

abstract class ResourceSelectDialog[SpecType, T, MT](
  owner: Window, 
  sm: StateMaster,
  initialSelectionOpt: Option[SpecType],
  allowNone: Boolean,
  metaResource: MetaResource[T, MT]) 
  extends StdDialog(owner, "Select" + metaResource.rcType.capitalize) {

  def onSuccess(result: Option[SpecType]): Unit
 
  def okFunc(): Unit = {
    onSuccess(resourceSelector.curSelection)
    close()
  }
  
  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType
  
  def rightPaneFor(
    selection: SpecType,
    updateSelectionF: SpecType => Unit): Component =
      new BoxPanel(Orientation.Vertical)
  
  val resourceSelector = new ResourceSelectPanel[SpecType, T, MT](
    sm,
    initialSelectionOpt,
    allowNone,
    metaResource) {
    
    def specToResourceName(spec: SpecType): String =
      ResourceSelectDialog.this.specToResourceName(spec)
    def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType =
      ResourceSelectDialog.this.newRcNameToSpec(name, prevSpec)
      
    override def rightPaneFor(
      selection: SpecType,
      updateSelectionF: SpecType => Unit): Component = {
      ResourceSelectDialog.this.rightPaneFor(selection, updateSelectionF)
    }
  }
  
  contents = new DesignGridPanel {
    row().grid().add(resourceSelector)
    addButtons(cancelBtn, okBtn)
  }
}