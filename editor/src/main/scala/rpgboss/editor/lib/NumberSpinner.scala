package rpgboss.editor.lib

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
  val model = new SpinnerNumberModel(initial, min, max, step)

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