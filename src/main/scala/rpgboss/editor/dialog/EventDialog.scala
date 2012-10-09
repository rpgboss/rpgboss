package rpgboss.editor.dialog

import scala.swing._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.lib.DesignGridPanel
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page

class EventDialog(
    owner: Window, 
    initialEvent: RpgEvent, 
    onOk: RpgEvent => Any,
    onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + initialEvent.name) {

  var event = initialEvent
  val evtStates = new ArrayBuffer() ++ event.states
  
  def paneForEvtState(idx: Int) = {
    def curEvtState = evtStates(idx)
    
    new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        val triggerBox = new ComboBox(EventTrigger.values.toSeq)
        
        row().grid().add(leftLabel("Trigger:"))
        row().grid().add(triggerBox)
        
        reactions += {
          case SelectionChanged(`triggerBox`) =>
            evtStates.update(
                idx, curEvtState.copy(trigger = triggerBox.selection.item))
        }
      }
    } 
  }
  
  def okFunc() = {
    onOk(event)
    close()
  }
  
  override def cancelFunc() = {
    onCancel(event)
    super.cancelFunc()
  }
  
  val nameField = new TextField {
    columns = 12
    text = event.name
  }
  
  val tabPane = new TabbedPane() {
    for(i <- 0 until evtStates.length)
      pages += new Page("State %d".format(i+1), paneForEvtState(i))
  }
  
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid()
        .add(leftLabel("Name:")).add(nameField)
        .add(
            new Button(Action("Add state") {
              // Add to list of states
              evtStates.append(evtStates.last.copy(cmds = Array()))
              // Add tabpane for it
              tabPane.pages += new Page(
                  "State %d".format(evtStates.length), 
                  paneForEvtState(evtStates.length-1))
            }))          
      
      row.grid().add(tabPane)
      
      addButtons(cancelButton, okButton)
    }
  }

  listenTo(nameField)
  reactions += {
    case EditDone(`nameField`) =>
      event = event.copy(name = nameField.text)
      peer.setTitle("Event: " + event.name)
  }
}