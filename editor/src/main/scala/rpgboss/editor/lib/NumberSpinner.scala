package rpgboss.editor.lib

import scala.swing._
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class NumberSpinner(initial: Int, min: Int, max: Int, step: Int = 1) 
  extends BoxPanel(Orientation.Horizontal)
{
  val model = new SpinnerNumberModel(initial, min, max, step)
  
  contents += Component.wrap(new JSpinner(model))

  def getValue = model.getNumber().intValue()
}