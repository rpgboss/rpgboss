package rpgboss.editor.dialog

import scala.swing._
import rpgboss.model.event._

class EventDialog(
    owner: Window, 
    event: RpgEvent, 
    onOk: RpgEvent => Any,
    onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + event.label) {

  def okFunc() = {
    onOk(event)
    close()
  }
  
  override def cancelFunc() = {
    onCancel(event)
    super.cancelFunc()
  }
}