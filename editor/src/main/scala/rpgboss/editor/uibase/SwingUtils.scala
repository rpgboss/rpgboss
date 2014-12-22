package rpgboss.editor.uibase

import rpgboss.lib._
import rpgboss.model._
import scala.collection.mutable.Buffer
import scala.swing._
import scala.swing.event._
import javax.swing.ImageIcon

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

  def standardIdxRenderer[A, B](labelF: A => B)
    (implicit renderer: ListView.Renderer[B]) =
      customIdxRenderer((a: A, idx: Int) =>
        StringUtils.standardIdxFormat(idx, labelF(a).toString))

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
      minimumSize = new Dimension(100, 1)
      text = initial
      listenTo(this)
      reactions += {
        case ValueChanged(_) =>
          onUpdate(text)
          additionalAction.foreach(_.apply())
      }
    }

  def textAreaField(initial: Array[String], onUpdate: Array[String] => Unit) = {
    val textEdit = new TextArea(initial.mkString("\n")) {
      listenTo(this)
      reactions += {
        case e: ValueChanged => onUpdate(text.split("\n"))
      }
    }

    new ScrollPane {
      contents = textEdit
      preferredSize = new Dimension(300, 150)
    }
  }

  def percentField(min: Float, max: Float, initial: Float,
      onUpdate: Float => Unit) = {
    val spinner = new NumberSpinner(
      (min * 100).toInt,
      (max * 100).toInt,
      (initial * 100).round,
      v => onUpdate(v.toFloat / 100))

    new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("%") {
        preferredSize = new Dimension(20, 15)
      }

      def value = spinner.getValue.toFloat / 100f
      def setValue(v: Float) = spinner.setValue((v * 100).round)
      override def enabled_=(b: Boolean) {
        super.enabled_=(b)
        spinner.enabled_=(b)
      }
    }
  }

  def pxField(min: Int, max: Int, initial: Int, onUpdate: Int => Unit) = {
    val spinner = new NumberSpinner(min, max, initial, onUpdate)
    new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("px") {
        preferredSize = new Dimension(20, 15)
      }

      def value = spinner.getValue
      def setValue(v: Float) = spinner.setValue(v.round)
      override def enabled_=(b: Boolean) {
        super.enabled_=(b)
        spinner.enabled_=(b)
      }
    }
  }

  /**
   * Accepts any types <: that are 'viewable' i.e. implicitly convertible to
   * HasName.
   */
  def indexedCombo[T <% HasName](
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
    overrideChoiceSet: Option[Seq[enum.Value]] = None,
    customRenderer: Option[enum.Value => Any] = None) = {

    val choices = overrideChoiceSet.getOrElse(enum.values.toSeq)

    new ComboBox(choices) {
      selection.item = enum(initialId)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          onUpdate(selection.item.id)
          additionalAction.foreach(_.apply())
      }

      if (customRenderer.isDefined) {
        renderer = ListView.Renderer(customRenderer.get)
      }
    }
  }

  def openEnumSelectDialog[T <: Enumeration](enum: T)(
    owner: Window,
    windowTitle: String,
    onSelect: enum.Value => Any) = {
    val d = new StdDialog(owner, windowTitle) {
      // Noop, as there is no okay button
      def okFunc() = {}

      contents = new BoxPanel(Orientation.Vertical) {
        enum.values.foreach { value =>
          contents += new Button(Action(value.toString) {
            onSelect(value)
            close()
          })
        }
        contents += new DesignGridPanel {
          addCancel(cancelBtn)
        }
      }
    }
    d.open()
  }

  def makeButtonGroup(btns: Seq[AbstractButton]) = {
    val firstSelected = btns.find(_.selected)
    val group = new ButtonGroup(btns: _*)
    firstSelected.map { btn => group.select(btn) }
    group
  }

  def addBtnsAsGrp(contents: Buffer[Component], btns: Seq[AbstractButton]) = {
    val group = makeButtonGroup(btns)
    contents ++= btns
  }

  def enumButtons[T <: Enumeration](enum: T)(
      initial: enum.Value,
      selectF: enum.Value => Any,
      iconPaths: List[String]) =
  {
    val enumValues = enum.values.toList

    assert(iconPaths.isEmpty || iconPaths.length == enumValues.length)

    enumValues.zipWithIndex.map { case (eVal, i) =>
      new ToggleButton() {
        action = Action(if (iconPaths.isEmpty) eVal.toString else "") {
          selectF(eVal)
        }

        if (!iconPaths.isEmpty) {
          icon = new ImageIcon(Utils.readClasspathImage(iconPaths(i)))
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

  def boolEnumHorizBox(
    enum: BooleanRpgEnum,
    initial: Boolean,
    onUpdate: Boolean => Any) = {
    val radios = enumIdRadios(enum)(
      AddOrRemove.fromBoolean(initial).id,
      id => onUpdate(enum.toBoolean(id)))

    new BoxPanel(Orientation.Horizontal) {
      addBtnsAsGrp(contents, radios)
    }
  }

  def enumVerticalBox(
    enum: RpgEnum,
    initial: Int,
    onUpdate: Int => Any) = {
    new BoxPanel(Orientation.Vertical) {
      addBtnsAsGrp(contents, enumIdRadios(enum)(initial, onUpdate))
    }
  }

  def showErrorDialog(parent: Component, message: String) = {
    Dialog.showMessage(parent, message, "Error", Dialog.Message.Error)
  }
}