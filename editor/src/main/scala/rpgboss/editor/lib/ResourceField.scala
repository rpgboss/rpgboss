package rpgboss.editor.lib

import scala.swing._
import rpgboss.editor._
import rpgboss.model.resource._
import rpgboss.model._
import scala.swing.event.MouseClicked

class WindowskinSelectDialog(
    owner: Window, 
    sm: StateMaster,
    initialSelectionOpt: Option[String],
    onSuccess: (Option[String]) => Any)
  extends ResourceSelectDialog(
      owner,
      sm,
      initialSelectionOpt,
      onSuccess,
      false,
      Windowskin)
{
  def specToResourceName(spec: String) = spec
  def newRcNameToSpec(name: String, prevSpec: Option[String]) = name
  
  def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Windowskin.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img)
  }
}

class WindowskinField(
    owner: Window, 
    sm: StateMaster,
    initial: String) 
  extends BoxPanel(Orientation.Horizontal) {
  
  val fieldName = new TextField {
    text = initial
    editable = false
    enabled = true
  }
  
  def initialOpt = if(initial.isEmpty()) None else Some(initial)
  
  val browseBtn = new Button(Action("...") {
    val diag = new WindowskinSelectDialog(
        owner, sm, Some(fieldName.text), 
        newOpt => fieldName.text = newOpt.getOrElse("")
    )
    diag.open()
  })
  
  listenTo(fieldName.mouse.clicks)
      
  reactions += {
    case MouseClicked(`fieldName`, _, _, _, _) => 
      browseBtn.action.apply()
  }
  
  def text = fieldName.text
  
  contents += fieldName
  contents += browseBtn
}