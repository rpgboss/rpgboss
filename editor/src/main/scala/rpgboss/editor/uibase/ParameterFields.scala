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

class ParameterDialog[T](
    owner: Window,
    initial: EventParameter[T],
    onOk: EventParameter[T] => Unit)
    (implicit m: reflect.Manifest[EventParameter[T]])
    extends StdDialog(owner, "Parameter") {

  val model = Utils.deepCopy(initial)

  val fLocalVariable = textField(model.localVariable, model.localVariable = _)

  def updateFields() = {
    fLocalVariable.enabled =
      model.valueTypeId == EventParameterValueType.LocalVariable.id
  }
  updateFields()

  val valueTypeBtns = enumIdRadios(EventParameterValueType)(
      model.valueTypeId,
      v => {
        model.valueTypeId = v
        updateFields()
      })
  val group = makeButtonGroup(valueTypeBtns)

  def okFunc() = {
    onOk(model)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(valueTypeBtns(0))
    row().grid().add(Swing.HGlue)
    row().grid().add(valueTypeBtns(1))
    row().grid().add(fLocalVariable)
    addButtons(okBtn, cancelBtn)
  }
}

class ParameterFullComponent[T](
    owner: Window,
    model: EventParameter[T],
    component: Component)
    (implicit m: reflect.Manifest[EventParameter[T]])
      extends BoxPanel(Orientation.Horizontal) {
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
          "Local Variable: %s".format(model.localVariable)
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
      val fullComponent = new ParameterFullComponent(
          owner, field.model, field.getModelComponent())
      panel.row().grid((new Label(field.name)).peer).add(fullComponent.peer)
    }
  }
}
