package rpgboss.editor.uibase

import scala.swing.Component
import scala.swing.Window
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.lib.Utils
import rpgboss.model.HasName
import rpgboss.model.PictureSlots
import rpgboss.model.ProjectData
import rpgboss.model.event._
import rpgboss.editor.Internationalized._
import rpgboss.player.RpgScreen
import rpgboss.editor.dialog.cmd.EventCmdUI
import rpgboss.editor.StateMaster

/**
 * The name of the field and a component for editing the constant value.
 */
abstract class EventParameterField[T](
    val name: String, val model: EventParameter[T])
    (implicit m: reflect.Manifest[T]) {
  def constantComponentFactory(p: EventParameter[T]): Component

  def getModelComponent() = constantComponentFactory(model)

  def getModelCopyComponent() = {
    val modelCopy = Utils.deepCopy(model)
    val component = constantComponentFactory(modelCopy)
    (modelCopy, component)
  }
}

object EventParameterField {
  def BooleanField(name: String, model: BooleanParameter) =
    new EventParameterField[Boolean](name, model) {
      override def constantComponentFactory(p: EventParameter[Boolean]) =
        // Do not pass name parameter, as label will be displayed on left.
        boolField("", model.constant, model.constant = _)
    }

  def IntNumberField(
      name: String, min: Int, max: Int, model: IntParameter,
      additionalAction: Option[() => Unit] = None) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        new NumberSpinner(
            min, max, p.constant, p.constant = _, additionalAction)
    }

  def IntEnumIdField[T <: HasName](
      name: String, choices: Array[T], model: IntParameter,
      additionalAction: Option[() => Unit] = None) =
    new EventParameterField[Int](name, model) {
      override def constantComponentFactory(p: EventParameter[Int]) =
        indexedCombo(choices, p.constant, p.constant = _, additionalAction)
    }

  def FloatField(
      name: String, min: Float, max: Float, model: FloatParameter) =
    new EventParameterField[Float](name, model) {
      override def constantComponentFactory(p: EventParameter[Float]) =
        new FloatSpinner(min, max, 0.1f, p.constant, p.constant = _)
    }

  def FloatPercentField(
      name: String, min: Float, max: Float,
      model: FloatParameter) =
    new EventParameterField[Float](name, model) {
      override def constantComponentFactory(p: EventParameter[Float]) =
        percentField(min, max, p.constant, p.constant =_)
    }

  def IntMultiselectField[T <: HasName](
      owner: Window,
      name: String,
      choices: Array[T],
      model: IntArrayParameter) =
    new EventParameterField[Array[Int]](name, model) {
      override def constantComponentFactory(p: EventParameter[Array[Int]]) =
        new ArrayMultiselectPanel(owner, name, choices, p.constant,
            p.constant = _)
    }

  def StringField(
      name: String, model: StringParameter,
      additionalAction: Option[() => Unit] = None) =
    new EventParameterField[String](name, model) {
      override def constantComponentFactory(p: EventParameter[String]) =
        textField(p.constant, p.constant = _, additionalAction,
            skipSizing = true)
    }

  def getParameterFields(
      owner: Window, sm: StateMaster, cmd: EventCmd):
      Seq[EventParameterField[_]] = {
    val ui = EventCmdUI.uiFor(cmd)
    if (ui == null)
      Nil
    else
      ui.getParameterFields(owner, sm, None, cmd.asInstanceOf[ui.EventCmdType])
  }
}