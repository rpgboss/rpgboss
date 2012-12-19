package rpgboss.editor.lib

import scala.swing._
import rpgboss.editor._
import rpgboss.model.resource._
import rpgboss.model._
import scala.swing.event.MouseClicked

class StringSpecSelectDialog[M, MT](    
    owner: Window, 
    sm: StateMaster,
    initialSelectionOpt: Option[String],
    onSuccess: (Option[String]) => Any,
    allowNone: Boolean,
    metaResource: MetaResource[M, MT])
  extends ResourceSelectDialog(
      owner,
      sm,
      initialSelectionOpt,
      onSuccess,
      allowNone,
      metaResource) {
  def specToResourceName(spec: String) = spec
  def newRcNameToSpec(name: String, prevSpec: Option[String]) = name
}

class PictureSelectDialog(
    owner: Window, 
    sm: StateMaster,
    initialSelectionOpt: Option[String],
    onSuccess: (Option[String]) => Any)
  extends StringSpecSelectDialog(
      owner,
      sm,
      initialSelectionOpt,
      onSuccess,
      false,
      Picture)
{
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Picture.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img)
  }
}

class WindowskinSelectDialog(
    owner: Window, 
    sm: StateMaster,
    initialSelectionOpt: Option[String],
    onSuccess: (Option[String]) => Any)
  extends StringSpecSelectDialog(
      owner,
      sm,
      initialSelectionOpt,
      onSuccess,
      false,
      Windowskin)
{
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Windowskin.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img)
  }
}

abstract class StringBrowseField(
    owner: Window, 
    sm: StateMaster, 
    initial: String) 
  extends BoxPanel(Orientation.Horizontal) 
{
  
  val fieldName = new TextField {
    text = initial
    editable = false
    enabled = true
  }
  
  def initialOpt = if(initial.isEmpty()) None else Some(initial)
  
  def doBrowse()
  
  val browseBtn = new Button(Action("...") {
    doBrowse()
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

class WindowskinField(owner: Window, sm: StateMaster, initial: String) 
  extends StringBrowseField(owner, sm, initial) 
{
  def doBrowse() = {
    val diag = new WindowskinSelectDialog(
        owner, sm, Some(fieldName.text), 
        newOpt => fieldName.text = newOpt.getOrElse("")
    )
    diag.open()
  }
}

class PictureField(owner: Window, sm: StateMaster, initial: String) 
  extends StringBrowseField(owner, sm, initial) 
{
  def doBrowse() = {
    val diag = new PictureSelectDialog(
        owner, sm, Some(fieldName.text), 
        newOpt => fieldName.text = newOpt.getOrElse("")
    )
    diag.open()
  }
}

class MsgfontField(owner: Window, sm: StateMaster, initial: String) 
  extends StringBrowseField(owner, sm, initial) 
{
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
        owner, sm, Some(fieldName.text), 
        newOpt => fieldName.text = newOpt.getOrElse(""),
        false, Msgfont)
    diag.open()
  }
}

class SoundField(owner: Window, sm: StateMaster, initial: String) 
  extends StringBrowseField(owner, sm, initial) 
{
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
        owner, sm, Some(fieldName.text), 
        newOpt => fieldName.text = newOpt.getOrElse(""),
        true, Sound)
    diag.open()
  }
}