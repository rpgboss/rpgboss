package rpgboss.editor.uibase

import scala.swing._
import scala.collection.mutable.Buffer
import rpgboss.model._
import scala.swing.event._
import scala.swing._

object SwingUtils {
  def lbl(s: String) = new Label(s)
  def leftLabel(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }

  def boolField(initial: Boolean, onUpdate: Boolean => Unit, text: String = "") =
    new CheckBox(text) {
      selected = initial
      listenTo(this)
      reactions += {
        case ButtonClicked(_) => onUpdate(selected)
      }
    }

  def textField(initial: String, onUpdate: String => Unit) =
    new TextField {
      text = initial
      listenTo(this)
      reactions += {
        case ValueChanged(_) => onUpdate(text)
      }
    }
  
  trait FloatSlider {
    def stepsPerOne: Int
    def value: Int
    def floatValue: Float = value.toFloat / stepsPerOne
  }
  
  def floatSlider(initial: Float, minArg: Float, maxArg: Float,
                  stepsPerOneArg: Int, minorStepArg: Int, majorStepArg: Int,  
                  onUpdate: Float => Unit) = {
    new Slider with FloatSlider {
      def stepsPerOne = stepsPerOneArg
      
      min = (minArg * stepsPerOne).toInt
      max = (maxArg * stepsPerOne).toInt
      minorTickSpacing = minorStepArg
      majorTickSpacing = majorStepArg
      snapToTicks = true
      paintLabels = true
      value = (initial * stepsPerOne).toInt
      
      listenTo(this)
      reactions += {
        case ValueChanged(_) => onUpdate(floatValue)
      }
    }
  }
  
  def slider(initial: Int, minArg: Int, maxArg: Int, 
             minorStepArg: Int, majorStepArg: Int,  
             onUpdate: Int => Unit) = {
    new Slider {
      min = minArg
      max = maxArg
      minorTickSpacing = minorStepArg
      majorTickSpacing = majorStepArg
      snapToTicks = true
      paintLabels = true
      value = initial
      
      listenTo(this)
      reactions += {
        case ValueChanged(_) => onUpdate(value)
      }
    }
  }

  def indexedCombo[T](choices: Seq[T], initial: Int, onUpdate: Int => Unit) = {
    new ComboBox(choices) {
      selection.index = initial

      renderer = ListView.Renderer(choice => {
        val idx = choices.indexOf(choice)
        "%d: %s".format(idx, choice.toString())
      })

      reactions += {
        case SelectionChanged(_) => onUpdate(selection.index)
      }
    }
  }

  def enumCombo[T <: Enumeration](enum: T)(
    initialId: Int,
    onUpdate: enum.Value => Any,
    choices: Seq[enum.Value] = Seq()) = {

    val actualChoices = if (choices.isEmpty) enum.values.toSeq else choices

    new ComboBox(actualChoices) {
      selection.item = enum(initialId)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) => onUpdate(selection.item)
      }
    }
  }

  def addBtnsAsGrp(contents: Buffer[Component], btns: Seq[AbstractButton]) = {
    val firstSelected = btns.find(_.selected)
    val grp = new ButtonGroup(btns: _*)

    contents ++= btns

    firstSelected.map { btn => grp.select(btn) }
  }

  def enumButtons[T <: Enumeration](enum: T)(initial: enum.Value, selectF: enum.Value => Any) =
    {
      enum.values.toList.map { eVal =>
        new ToggleButton() {
          action = Action(eVal.toString) {
            selectF(eVal)
          }

          selected = eVal == initial
        }
      }
    }

  def enumRadios[T <: Enumeration](enum: T)(
    initial: T#Value,
    selectF: T#Value => Any,
    choices: Seq[T#Value] = Seq()) =
    {
      val actualChoices = if (choices.isEmpty) enum.values.toSeq else choices
      actualChoices.map { eVal =>
        new RadioButton() {
          action = Action(eVal.toString) {
            selectF(eVal)
          }

          selected = eVal == initial
        }
      }
    }

}