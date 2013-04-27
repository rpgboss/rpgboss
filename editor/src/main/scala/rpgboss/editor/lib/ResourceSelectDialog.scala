package rpgboss.editor.lib

import scala.swing._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import com.typesafe.scalalogging.slf4j.Logging
import java.awt.Dimension
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.StdDialog
import scala.Array.canBuildFrom
import javax.swing.border.LineBorder

abstract class ResourceSelectDialogBase[SpecType, T, MT](
  owner: Window,
  metaResource: MetaResource[T, MT])
  extends StdDialog(
    owner,
    "Select " + metaResource.rcType.capitalize) {
  
  def onSuccess(result: Option[SpecType]): Unit
  
  def resourceSelector: ResourceSelectPanel[SpecType, T, MT]

  def okFunc(): Unit = {
    onSuccess(resourceSelector.curSelection)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(resourceSelector)
    addButtons(cancelBtn, okBtn)
  }
}

abstract class ResourceSelectDialog[SpecType, T, MT](
  owner: Window, 
  sm: StateMaster,
  initialSelectionOpt: Option[SpecType],
  allowNone: Boolean,
  metaResource: MetaResource[T, MT]) 
  extends ResourceSelectDialogBase[SpecType, T, MT](owner, metaResource) {

  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType
  
  def rightPaneFor(
    selection: SpecType,
    updateSelectionF: SpecType => Unit): Component
  
  override val resourceSelector = new ResourceSelectPanel[SpecType, T, MT](
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
}

class CustomResourceSelectDialog[SpecType, T, MT](
  owner: Window, 
  val resourceSelector: ResourceSelectPanel[SpecType, T, MT],
  onSuccessF: Option[SpecType] => Unit)
  extends ResourceSelectDialogBase[SpecType, T, MT](
    owner, 
    resourceSelector.metaResource) {
  
  override def onSuccess(result: Option[SpecType]) = onSuccessF(result)
  
}