package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.uibase._
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page
import rpgboss.model._
import rpgboss.editor.StateMaster
import java.awt.Dimension
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.resourceselector.SpriteField
import javax.swing.BorderFactory
import rpgboss.lib.Utils
import rpgboss.model.MapLoc
import rpgboss.model.event.RpgEvent

class EventInstanceDialog(
  owner: Window,
  sm: StateMaster,
  initialEvent: RpgEvent,
  onOk: RpgEvent => Any,
  onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event Instance: " + initialEvent.name) {

  val model = Utils.deepCopy(initialEvent)

  override def cancelFunc() = onCancel(model)

  def okFunc() = {
    onOk(model)
    close()
  }

  val fClassId = indexedCombo(
      sm.getProjData.enums.eventClasses,
      model.eventClassId,
      model.eventClassId = _,
      Some(() => {

      }))

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid(lbl("Event Class:")).add(fClassId)
      addButtons(okBtn, cancelBtn)
    }
  }
}