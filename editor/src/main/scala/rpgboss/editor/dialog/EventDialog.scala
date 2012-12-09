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
import rpgboss.editor.StateMaster

class EventDialog(
    owner: Window,
    sm: StateMaster,
    val mapName: String,
    initialEvent: RpgEvent, 
    onOk: RpgEvent => Any,
    onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event: " + initialEvent.name) {

  var event = initialEvent
  
  override def cancelFunc() = onCancel(event)
  
  class EventStatePane(val idx: Int) extends BoxPanel(Orientation.Horizontal) {
    def curEvtState = event.states(idx)
    
    val triggerBox = new ComboBox(EventTrigger.values.toSeq) {
      selection.item = EventTrigger(curEvtState.trigger)
    }
    val heightBox = new ComboBox(EventHeight.values.toSeq) {
      selection.item = EventHeight(curEvtState.height)
    }
    val spriteBox = new SpriteBox(
        owner, 
        sm.getProj, 
        curEvtState.sprite, 
        (spriteSpec: Option[SpriteSpec]) => {
          // If the sprite's "existence" has changed...
          if(curEvtState.sprite.isDefined != spriteSpec.isDefined) {
            heightBox.selection.item = if(spriteSpec.isDefined) 
              EventHeight.SAME else EventHeight.UNDER
          }
        })
    
    contents += new DesignGridPanel {
      row().grid().add(leftLabel("Trigger:"))
      row().grid().add(triggerBox)
      row().grid().add(leftLabel("Height:"))
      row().grid().add(heightBox)
      row().grid().add(leftLabel("Sprite:"))
      row().grid().add(spriteBox)
    }
    
    val commandBox = new CommandBox(
        EventDialog.this,
        owner, 
        sm,
        curEvtState.cmds)
    
    contents += new DesignGridPanel {
      row.grid.add(leftLabel("Commands:"))
      row.grid.add(new ScrollPane {
        preferredSize = new Dimension(400, 400)
        contents = commandBox
      })
    }
    
    def formToModel() = {
      val origState = event.states(idx)
      val newState = origState.copy(
          sprite = spriteBox.spriteSpecOpt,
          trigger = triggerBox.selection.item.id,
          height = heightBox.selection.item.id,
          cmds = commandBox.listData.toArray
      )
      event.states.update(idx, newState)
    }
  }
  
  def okFunc() = {
    event = event.copy(name = nameField.text)
    
    tabPane.pages.foreach { page =>
      val pane = page.content.asInstanceOf[EventStatePane]
      pane.formToModel()
    }
    
    onOk(event)
    close()
  }
  
  val nameField = new TextField {
    columns = 12
    text = event.name
  }
  
  val tabPane = new TabbedPane() {
    for(i <- 0 until event.states.length) {
      val state = event.states(i)
      if(state.deleted == false) {
        pages += new Page("State %d".format(i+1), new EventStatePane(i))
      }
    }
  }
  
  def curPane = tabPane.selection.page.content.asInstanceOf[EventStatePane]
  
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid()
        .add(leftLabel("Name:")).add(nameField)
        .add(
            new Button(Action("Add state") {
              // Save the current pane
              curPane.formToModel()
              
              // Add to list of states
              val newState = event.states(curPane.idx).copy(
                  cmds = RpgEventState.defaultCmds)
              event = event.copy(states = event.states ++ Array(newState))
              // Add tabpane for it
              tabPane.pages += new Page(
                  "State %d".format(event.states.length), 
                  new EventStatePane(event.states.length-1))
              tabPane.selection.page = tabPane.pages.last
            }))
        .add(
            new Button(Action("Delete state") {
              // final save of the current state of the event.
              curPane.formToModel()
              
              val stateIdx = curPane.idx
              
              // Set state to deleted
              event.states.update(
                  stateIdx, 
                  event.states(stateIdx).copy(deleted = true))
              
              // Remove deleted page from tab pane
              tabPane.pages -= tabPane.selection.page
            })
        )
      
      row.grid().add(tabPane)
      
      addButtons(cancelBtn, okBtn)
    }
  }
}