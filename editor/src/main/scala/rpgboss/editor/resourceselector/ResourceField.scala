package rpgboss.editor.resourceselector

import scala.swing._
import rpgboss.editor._
import rpgboss.model.resource._
import rpgboss.model._
import scala.swing.event.MouseClicked
import scala.swing.event.EditDone
import com.typesafe.scalalogging.slf4j.Logging
import rpgboss.editor.uibase.ImagePanel

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

abstract class BrowseField[SpecType](
  owner: Window,
  sm: StateMaster,
  initial: Option[SpecType],
  onUpdate: Option[SpecType] => Unit)
  extends BoxPanel(Orientation.Horizontal) with Logging {
  
  var model = initial
  
  val textField = new TextField {
    editable = false
    enabled = true
  }
  
  def updateWidgets() =
    textField.text = model.map(_.toString).getOrElse("<None>")
  
  updateWidgets()
  
  def doBrowse()

  val browseBtn = new Button(Action("...") {
    doBrowse()
    logger.debug("Post-browse button onUpdate")
    updateWidgets()
    onUpdate(model)
  })
  
  listenTo(textField.mouse.clicks)

  reactions += {
    case MouseClicked(`textField`, _, _, _, _) =>
      browseBtn.action.apply()
  }

  contents += textField
  contents += browseBtn
}

abstract class StringBrowseField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends BrowseField[String](
    owner,
    sm,
    if(initial.isEmpty()) None else Some(initial),
    result => onUpdate(result.getOrElse("")))

class WindowskinField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def doBrowse() = {
    val diag = new WindowskinSelectDialog(
      owner, sm, model, model = _)
    diag.open()
  }
}

class PictureField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def doBrowse() = {
    val diag = new PictureSelectDialog(
      owner, sm, model, model = _)
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
      owner, sm, model,
      false, Msgfont,
      model = _)
    diag.open()
  }
}