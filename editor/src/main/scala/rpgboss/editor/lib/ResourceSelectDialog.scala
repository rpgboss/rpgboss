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

class ResourceSelectDialog[SpecType, T, MT](
  owner: Window,
  resourceSelector: ResourceSelectPanel[SpecType, T, MT],
  onSuccess: Option[SpecType] => Unit)
  extends StdDialog(
    owner,
    "Select " + resourceSelector.metaResource.rcType.capitalize) {

  def okFunc(): Unit = {
    onSuccess(resourceSelector.curSelection)
    close()
  }
  
  contents = new DesignGridPanel {
    row().grid().add(resourceSelector)
    addButtons(cancelBtn, okBtn)
  }
}