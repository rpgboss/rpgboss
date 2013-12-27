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

  /**
   * General form that can be used for unusual index positioning.
   */
  def customIdxRenderer[A, B](f: (A, Int) => B)
    (implicit renderer: ListView.Renderer[B]): ListView.Renderer[A] =
    new ListView.Renderer[A] {
      def componentFor(
        list: ListView[_],
        isSelected: Boolean,
        focused: Boolean,
        a: A,
        indexArgument: Int): Component = {

        // Normalize for case of selected combobox. (Otherwise -1 shown).
        val index = if (indexArgument < 0)
          list.selection.indices.head
        else
          indexArgument

        renderer.componentFor(list, isSelected, focused, f(a, index), index)
      }
    }

  /**
   * Standard "42: Name" format.
   */
  def standardIdxRenderer[A, B](labelF: A => B)
    (implicit renderer: ListView.Renderer[B]) =
      customIdxRenderer((a: A, idx: Int) => "%d: %s".format(idx, labelF(a)))

  def boolField(text: String, initial: Boolean, onUpdate: Boolean => Unit,
                additionalAction: Option[() => Unit] = None) =
    new CheckBox(text) {
      selected = initial
      listenTo(this)
      reactions += {
        case ButtonClicked(_) =>
          onUpdate(selected)
          additionalAction.foreach(_.apply())
      }
    }

  def textField(initial: String, onUpdate: String => Unit,
                additionalAction: Option[() => Unit] = None) =
    new TextField {
      text = initial
      listenTo(this)
      reactions += {
        case ValueChanged(_) =>
          onUpdate(text)
          additionalAction.foreach(_.apply())
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

  def indexedComboStrings(choices: Seq[String], initial: Int,
                          onUpdate: Int => Unit,
                          additionalAction: Option[() => Unit] = None) = {
    new ComboBox(choices) {
      selection.index = initial
      renderer = standardIdxRenderer(x => x)

      reactions += {
        case SelectionChanged(_) =>
          onUpdate(selection.index)
          additionalAction.foreach(_.apply())
      }
    }
  }

  def indexedCombo[T <: HasName](
    choices: Seq[T], initial: Int, onUpdate: Int => Unit,
    additionalAction: Option[() => Unit] = None) = {
    new ComboBox(choices) {
      selection.index = initial
      renderer = standardIdxRenderer(_.name)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          onUpdate(selection.index)
          additionalAction.foreach(_.apply())
      }
    }
  }

  def enumIdCombo[T <: Enumeration](enum: T)(
    initialId: Int,
    onUpdate: Int => Any,
    additionalAction: Option[() => Unit] = None,
    overrideChoiceSet: Option[Seq[enum.Value]] = None) = {

    val choices = overrideChoiceSet.getOrElse(enum.values.toSeq)

    new ComboBox(choices) {
      selection.item = enum(initialId)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          onUpdate(selection.item.id)
          additionalAction.foreach(_.apply())
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

  def enumIdRadios[T <: Enumeration](enum: T)(
    initialId: Int,
    onUpdate: Int => Any,
    choices: Seq[T#Value] = Seq(),
    disabledSet: Set[T#Value] = Set[T#Value]()) =
    {
      val actualChoices = if (choices.isEmpty) enum.values.toSeq else choices
      actualChoices.map { eVal =>
        new RadioButton() {
          action = Action(eVal.toString) {
            onUpdate(eVal.id)
          }

          enabled = !disabledSet.contains(eVal)
          selected = enabled && eVal.id == initialId
        }
      }
    }

}