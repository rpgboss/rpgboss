package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.StateMaster

class MoveEventCmdDialog(
  owner: Window,
  sm: StateMaster,
  mapName: String,
  initial: MoveEvent,
  successF: (MoveEvent) => Any)
  extends StdDialog(owner, "Move event") {

  var model = initial
  
  val mapData = sm.getMapData(mapName)
  
  val fieldWhichEvent = new EntitySelectPanel(owner, sm, mapData, 
                                              model.entitySpec, 
                                              model.entitySpec = _)
  
  val fieldDx = new FloatSpinner(model.dx, -100, 100, model.dx = _, 0.1f)
  val fieldDy = new FloatSpinner(model.dy, -100, 100, model.dy = _, 0.1f)
  
  val fieldChangeDirection = 
    boolField(model.changeDirection, model.changeDirection = _)
  val fieldAwaitDone = 
    boolField(model.awaitDone, model.awaitDone = _)

  def okFunc() = {
    successF(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid(lbl("Event:")).add(fieldWhichEvent)
    row().grid(lbl("X Movement:")).add(fieldDx)
    row().grid(lbl("Y Movement:")).add(fieldDy)
    row().grid(lbl("Change direction:")).add(fieldChangeDirection)
    row().grid(lbl("Await done:")).add(fieldAwaitDone)
    
    addButtons(cancelBtn, okBtn)
  }

}