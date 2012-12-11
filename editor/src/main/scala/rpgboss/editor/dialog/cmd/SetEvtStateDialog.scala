package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog.StdDialog
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.model.RpgEnum
import rpgboss.model.EvtPath
import rpgboss.editor.lib.NumberSpinner

class SetEvtStateDialog(
    owner: Window, 
    initial: SetEvtState, 
    successF: (SetEvtState) => Any) 
  extends StdDialog (owner, "Set Event State") {

  import WhichEvent._
  var whichEvent = WhichEvent(initial.whichEvtId)
  
  var listOfToggleFields: List[ToggledTextField] = Nil
  
  class ToggledTextField(
      val minWhichEvt: WhichEvent.Value,
      defaultStr: String,
      initialF: (EvtPath => String)) extends TextField() 
  {
    text = {
      if(whichEvent.id >= minWhichEvt.id)
        initial.evtPathOpt.map(initialF(_)).getOrElse("")
      else ""
    }
    
    def toggle() = {
      if(whichEvent.id >= minWhichEvt.id) {
        text = ""
        enabled = true
      } else {
        text = defaultStr
        enabled = false
      }
    }
    
    listOfToggleFields = this :: listOfToggleFields
  }
  
  val fieldEvtName = 
    new ToggledTextField(SAMEMAPEVENT, "This event",  _.evtName)
  val fieldMapName = 
    new ToggledTextField(OTHERMAPEVENT, "This map", _.mapName)
  
  // Important: This dialog is one-based, whereas the actual program is 0-based
  val fieldNewState = new NumberSpinner(initial.state, 0, 127, 1)
  
  def updateFieldsState() = {
    listOfToggleFields.foreach(_.toggle())
  }
  updateFieldsState()
  
  contents = new DesignGridPanel {
    val btns = enumRadios(WhichEvent)(whichEvent, { eVal =>
      whichEvent = eVal
      updateFieldsState()
    })
    
    def lbl(s: String) = new Label(s)
    
    row().grid(lbl("Which Event:")).add(new BoxPanel(Orientation.Vertical) {
      addBtnsAsGrp(contents, btns)
    })
    row().grid(lbl("Map name:")).add(fieldMapName)
    row().grid(lbl("Event name:")).add(fieldEvtName)
    row().grid(lbl("New state:")).add(fieldNewState)
    
    addButtons(cancelBtn, okBtn)
  }
  
  def okFunc() = {
    val errors = whichEvent match {
      case SAMEMAPEVENT  => 
        if(fieldEvtName.text.isEmpty()) {
          Dialog.showMessage(okBtn, "No event selected", "Error", 
                   Dialog.Message.Error)
          true
        } else false
      case OTHERMAPEVENT =>
        if(fieldMapName.text.isEmpty()) {
          Dialog.showMessage(okBtn, "No map selected", "Error", 
                   Dialog.Message.Error)
          true
        }
        else if(fieldEvtName.text.isEmpty()) {
          Dialog.showMessage(okBtn, "No event selected", "Error", 
                   Dialog.Message.Error)
          true
        } else false
      case _ => false
    }
    
    if(!errors) {
    
      val path = whichEvent match {
        case SAMEMAPEVENT  => 
          Some(EvtPath("", fieldEvtName.text))
        case OTHERMAPEVENT => 
          Some(EvtPath(fieldMapName.text, fieldEvtName.text))
        case _ => None
      }
      
      val newState = fieldNewState.getValue 
      
      val cmd = SetEvtState(
          whichEvtId = whichEvent.id,
          evtPathOpt = path,
          state = newState)
      successF(cmd)
      
      close()
    }
  }
}