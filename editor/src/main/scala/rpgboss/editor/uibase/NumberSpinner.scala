package rpgboss.editor.uibase

import scala.swing._
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeListener
import javax.swing.event.ChangeEvent

class NumberSpinner(
  initial: Int,
  min: Int,
  max: Int,
  onUpdate: ((Int) => Unit) = (v) => {},
  step: Int = 1)
  extends BoxPanel(Orientation.Horizontal) {
  val normalizedInitial = math.min(math.max(initial, min), max)

  val model = new SpinnerNumberModel(normalizedInitial, min, max, step)

  val spinner = new JSpinner(model)

  spinner.addChangeListener(new ChangeListener() {
    override def stateChanged(e: ChangeEvent) {
      onUpdate(getValue)
    }
  })
  contents += Component.wrap(spinner)

  def getValue = model.getNumber().intValue()
  def setValue(v: Int) = model.setValue(v)

  override def enabled_=(b: Boolean) = {
    super.enabled_=(b)
    spinner.setEnabled(b)
  }
}

class FloatSpinner(
  initial: Float,
  min: Float,
  max: Float,
  onUpdate: ((Float) => Unit) = (v) => {},
  step: Float)
  extends BoxPanel(Orientation.Horizontal) {
  val normalizedInitial = math.min(math.max(initial, min), max)

  // TODO: Can't press down button to get to the minimum due to floating point
  // math business. Need to implement an epsilon compare or something.
  val model =
    new SpinnerNumberModel(normalizedInitial, min, max, step)

  val spinner = new JSpinner(model)

  spinner.addChangeListener(new ChangeListener() {
    override def stateChanged(e: ChangeEvent) {
      onUpdate(getValue)
    }
  })
  contents += Component.wrap(spinner)

  def getValue = model.getNumber().floatValue()
  def setValue(v: Float) = model.setValue(v)

  override def enabled_=(b: Boolean) = {
    super.enabled_=(b)
    spinner.setEnabled(b)
  }
}