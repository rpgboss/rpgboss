package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.lib.DesignGridPanel
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page
import rpgboss.model.Project
import rpgboss.model.SpriteSpec

class EventDialog(
    owner: Window,
    project: Project,
    initialEvent: RpgEvent, 
    onOk: RpgEvent => Any,
    onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + initialEvent.name) {

  var event = initialEvent
  
  override def cancelFunc() = onCancel(event)
  
  def paneForEvtState(idx: Int) = {
    def curEvtState = event.states(idx)
    def updateEvtState(evtState: RpgEventState) =
      event.states.update(idx, evtState)
    
    new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        val triggerBox = new ComboBox(EventTrigger.choices) {
          selection.index = curEvtState.trigger
        }
        val spriteBox = new SpriteBox(
            owner, 
            project, 
            curEvtState.sprite, 
            (spriteSpec: Option[SpriteSpec]) =>
              updateEvtState(curEvtState.copy(sprite = spriteSpec)))
        
        row().grid().add(leftLabel("Trigger:"))
        row().grid().add(triggerBox)
        row().grid().add(leftLabel("Sprite:"))
        row().grid().add(spriteBox)
        
        reactions += {
          case SelectionChanged(`triggerBox`) =>
            val selectedIdx = 
              EventTrigger.choices.indexOf(triggerBox.selection.item)
            updateEvtState(
                curEvtState.copy(trigger = selectedIdx))
        }
      }
      
      val commandBox = new CommandBox(
          owner, 
          project, 
          curEvtState.cmds,
          newCmds => updateEvtState(curEvtState.copy(cmds = newCmds)))
      
      contents += new DesignGridPanel {
        row.grid.add(leftLabel("Commands:"))
        row.grid.add(new ScrollPane {
          preferredSize = new Dimension(400, 400)
          contents = commandBox
        })
      }
    } 
  }
  
  def okFunc() = {
    onOk(event)
    close()
  }
  
  val nameField = new TextField {
    columns = 12
    text = event.name
  }
  
  val tabPane = new TabbedPane() {
    for(i <- 0 until event.states.length)
      pages += new Page("State %d".format(i+1), paneForEvtState(i))
  }
  
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid()
        .add(leftLabel("Name:")).add(nameField)
        .add(
            new Button(Action("Add state") {
              // Add to list of states
              val newState = event.states.last.copy(
                  cmds = RpgEventState.defaultCmds)
              event = event.copy(states = event.states ++ Array(newState))
              // Add tabpane for it
              tabPane.pages += new Page(
                  "State %d".format(event.states.length), 
                  paneForEvtState(event.states.length-1))
              tabPane.selection.page = tabPane.pages.last
            }))          
      
      row.grid().add(tabPane)
      
      addButtons(cancelBtn, okBtn)
    }
  }

  listenTo(nameField)
  reactions += {
    case EditDone(`nameField`) =>
      event = event.copy(name = nameField.text)
      peer.setTitle("Event: " + event.name)
  }
}