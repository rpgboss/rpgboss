package rpgboss.editor.uibase

import SwingUtils._
import scala.swing._
import rpgboss.model.event.IntParameter
import rpgboss.model.event.EventParameterValueType
import rpgboss.model.HasName
import scala.swing.event.MouseClicked
import rpgboss.model.event.EventParameter
import rpgboss.lib.Utils

class ParameterDialog[T](
    owner: Window,
    initial: EventParameter[T],
    onOk: EventParameter[T] => Unit,
    constantFieldFactory: EventParameter[T] => Component)
    (implicit m: reflect.Manifest[EventParameter[T]])
    extends StdDialog(owner, "Parameter") {

  val model = Utils.deepCopy(initial)

  val fConstant = constantFieldFactory(model)
  val fLocalVariable = textField(model.localVariable, model.localVariable = _)

  def updateFields() = {
    fConstant.enabled = model.valueTypeId == EventParameterValueType.Constant.id
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
    row().grid().add(fConstant)
    row().grid().add(valueTypeBtns(1))
    row().grid().add(fLocalVariable)
    addButtons(cancelBtn, okBtn)
  }
}

class ParameterField[T](
    owner: Window,
    model: EventParameter[T],
    constantFieldFactory: EventParameter[T] => Component)
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
        },
        constantFieldFactory)
    d.open()
  })
  var fConstant = constantFieldFactory(model)
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
      fConstant = constantFieldFactory(model)
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

class IntParameterNumberField(
    owner: Window,
    model: IntParameter,
    min: Int,
    max: Int)(implicit m: reflect.Manifest[IntParameter])
    extends ParameterField[Int](
        owner,
        model,
        model =>
          new NumberSpinner(model.constant, min, max, model.constant = _))

class IntParameterEnumerationIndexField[T <% HasName](
    owner: Window,
    model: IntParameter,
    choices: Seq[T])(implicit m: reflect.Manifest[IntParameter])
    extends ParameterField[Int](
        owner,
        model,
        model =>
          indexedCombo(choices, model.constant, model.constant = _))