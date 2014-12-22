package rpgboss.editor.uibase

import SwingUtils._
import scala.swing._
import rpgboss.model.event.IntParameter
import rpgboss.model.event.EventParameterValueType
import rpgboss.model.HasName
import scala.swing.event.MouseClicked
import rpgboss.model.event.EventParameter
import rpgboss.lib.Utils
import rpgboss.model.event.EventCmd
import rpgboss.model.ProjectData
import rpgboss.editor.Internationalized._

class ParameterDialog[T](
    owner: Window,
    initial: EventParameter[T],
    onOk: EventParameter[T] => Unit)
    (implicit m: reflect.Manifest[EventParameter[T]])
    extends StdDialog(owner, getMessage("Parameter")) {

  val model = Utils.deepCopy(initial)

  val fLocalVariable = textField(model.localVariable, model.localVariable = _)
  val fGlobalVariable =
    textField(model.globalVariable, model.globalVariable = _)

  def updateFields() = {
    fLocalVariable.enabled =
      model.valueTypeId == EventParameterValueType.LocalVariable.id
    fGlobalVariable.enabled =
      model.valueTypeId == EventParameterValueType.GlobalVariable.id

  }
  updateFields()

  val valueTypeBtns = enumIdRadios(EventParameterValueType)(
      model.valueTypeId,
      v => {
        model.valueTypeId = v
        updateFields()
      })
  val group = makeButtonGroup(valueTypeBtns)

  override def okFunc(): Unit = {
    import EventParameterValueType._
    if (model.valueTypeId == LocalVariable.id && model.localVariable.isEmpty) {
      Dialog.showMessage(
          fLocalVariable,
          getMessage("Local_Variable_Cannot_Be_Blank"),
          getMessage("Validation_Error"),
          Dialog.Message.Error)
      return
    }

    if (model.supportsGlobalVariable &&
        model.valueTypeId == GlobalVariable.id &&
        model.globalVariable.isEmpty) {
      Dialog.showMessage(
          fGlobalVariable,
          getMessage("Global_Variable_Cannot_Be_Blank"),
          getMessage("Validation_Error"),
          Dialog.Message.Error)
      return
    }
    onOk(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(valueTypeBtns(0))
    row().grid().add(Swing.HGlue)
    row().grid().add(valueTypeBtns(1))
    row().grid().add(fLocalVariable)

    if (model.supportsGlobalVariable) {
      row().grid().add(valueTypeBtns(2))
      row().grid().add(fGlobalVariable)
    }

    addButtons(okBtn, cancelBtn)
  }
}

class ParameterFullComponent[T](
    owner: Window,
    field: EventParameterField[T])
    (implicit m: reflect.Manifest[EventParameter[T]])
      extends BoxPanel(Orientation.Horizontal) {
  val model = field.model
  val component = field.getModelComponent()

  val container = new BoxPanel(Orientation.Horizontal)
  val detailsBtn = new Button(Action("...") {
    val d: ParameterDialog[T] = new ParameterDialog[T](
        owner,
        model,
        newModel => {
          model.copyValuesFrom(newModel)
          updateContainer()
        })
    d.open()
  })
  val fConstant = component
  val label = new TextField {
    editable = false
    enabled = true

    listenTo(this.mouse.clicks)
    reactions += {
      case e: MouseClicked => {
        detailsBtn.doClick()
      }
    }
  }

  def updateContainer(): Unit = {
    container.contents.clear()

    if (model.valueTypeId == EventParameterValueType.Constant.id) {
      container.contents += fConstant
    } else {
      container.contents += label
      label.text = EventParameterValueType(model.valueTypeId) match {
        case EventParameterValueType.LocalVariable =>
          getMessageColon("Local_Variable") + " %s".format(model.localVariable)
        case EventParameterValueType.GlobalVariable =>
          getMessageColon("Global_Variable") + " %s".format(model.globalVariable)
      }
    }

    container.revalidate()
    container.repaint()
  }
  updateContainer()

  contents += container
  contents += detailsBtn
}

object ParameterFullComponent {
  def addParameterFullComponentsToPanel(
      owner: Window,
      pData: ProjectData,
      panel: DesignGridPanel,
      cmd: EventCmd) = {
    for (field <- EventParameterField.getParameterFields(owner, pData, cmd)) {
      val fullComponent = new ParameterFullComponent(owner, field)
      panel.row().grid((new Label(field.name)).peer).add(fullComponent.peer)
    }
  }
}
