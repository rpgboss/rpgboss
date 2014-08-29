package rpgboss.editor.resourceselector

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase._
import com.typesafe.scalalogging.slf4j.LazyLogging

abstract class ResourceSelectDialog[SpecType, T, MT](
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[SpecType],
  allowNone: Boolean,
  metaResource: MetaResource[T, MT])
  extends StdDialog(owner, "Select" + metaResource.rcType.capitalize)
  with LazyLogging {

  minimumSize = new Dimension(550, 550)

  def onSuccess(result: Option[SpecType]): Unit

  def okFunc(): Unit = {
    onSuccess(resourceSelector.curSelection)
    close()
  }

  override def onClose() = {
    resourceSelector.dispose()
    super.onClose()
  }

  def specToResourceName(spec: SpecType): String
  def newRcNameToSpec(name: String, prevSpec: Option[SpecType]): SpecType

  def rightPaneFor(
    selection: SpecType,
    updateSelectionF: SpecType => Unit): DisposableComponent =
      new BoxPanel(Orientation.Vertical) with DisposableComponent

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
      updateSelectionF: SpecType => Unit) = {
      ResourceSelectDialog.this.rightPaneFor(selection, updateSelectionF)
    }
  }

  contents = new DesignGridPanel {
    row().grid().add(resourceSelector)
    addButtons(cancelBtn, okBtn)
  }
}