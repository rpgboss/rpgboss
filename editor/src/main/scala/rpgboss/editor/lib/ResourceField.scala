package rpgboss.editor.lib

import scala.swing._
import rpgboss.editor._
import rpgboss.model.resource._
import rpgboss.model._
import scala.swing.event.MouseClicked
import scala.swing.event.EditDone

class StringSpecSelectDialog[M, MT](
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  allowNone: Boolean,
  metaResource: MetaResource[M, MT],
  onSuccessF: (Option[String]) => Unit)
  extends ResourceSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    allowNone,
    metaResource) {
  override def specToResourceName(spec: String): String = spec
  override def newRcNameToSpec(name: String, prevSpec: Option[String]): String =
    name
    
  override def onSuccess(result: Option[String]) = onSuccessF(result)
}

class PictureSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  onSuccessF: (Option[String]) => Unit)
  extends StringSpecSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    Picture,
    onSuccessF) {
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Picture.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img)
  }
}

class WindowskinSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  onSuccessF: (Option[String]) => Unit)
  extends StringSpecSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    Windowskin,
    onSuccessF) {
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Windowskin.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img)
  }
}

abstract class StringBrowseField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends BoxPanel(Orientation.Horizontal) {

  val fieldName = new TextField {
    text = initial
    editable = false
    enabled = true
  }

  def initialOpt = if (initial.isEmpty()) None else Some(initial)

  def doBrowse()

  val browseBtn = new Button(Action("...") {
    doBrowse()
  })

  listenTo(fieldName)
  listenTo(fieldName.mouse.clicks)

  reactions += {
    case MouseClicked(`fieldName`, _, _, _, _) =>
      browseBtn.action.apply()
    case EditDone(`fieldName`) =>
      onUpdate(fieldName.text)
  }

  contents += fieldName
  contents += browseBtn
}

class WindowskinField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new WindowskinSelectDialog(
      owner, sm, Some(fieldName.text),
      newOpt => fieldName.text = newOpt.getOrElse(""))
    diag.open()
  }
}

class PictureField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new PictureSelectDialog(
      owner, sm, Some(fieldName.text),
      newOpt => fieldName.text = newOpt.getOrElse(""))
    diag.open()
  }
}

class MsgfontField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
      owner, sm, Some(fieldName.text),
      false, Msgfont,
      newOpt => fieldName.text = newOpt.getOrElse(""))
    diag.open()
  }
}

class SoundField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
      owner, sm, Some(fieldName.text),
      true, Sound,
      newOpt => fieldName.text = newOpt.getOrElse(""))
    diag.open()
  }
}