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
import rpgboss.editor.Internationalized._

class EventDialog(
  owner: Window,
  sm: StateMaster,
  val mapName: String,
  initialEvent: RpgEvent,
  onOk: RpgEvent => Any,
  onCancel: RpgEvent => Any)
  extends StdDialog(owner, getMessage("Event") + ": " + initialEvent.name) {

  centerDialog(new Dimension(600, 600))

  val event = Utils.deepCopy(initialEvent)

  override def cancelFunc() = onCancel(event)

  def okFunc() = {
    onOk(event)
    close()
  }

  val eventPanel = new EventPanel(
      owner,
      sm,
      Some(MapLoc(mapName, event.x, event.y)),
      event.name,
      event.name = _,
      event.states,
      event.states = _)

  contents = new BoxPanel(Orientation.Vertical) {
    contents += eventPanel
    contents += new DesignGridPanel {
      addButtons(okBtn, cancelBtn)
    }
  }
}