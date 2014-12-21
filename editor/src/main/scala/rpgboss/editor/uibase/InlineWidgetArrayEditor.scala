package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._
import java.awt.event.MouseEvent
import javax.swing.UIManager
import javax.swing.BorderFactory
import java.awt.Font
import scala.collection.mutable.ArrayBuffer
import rpgboss.editor.Internationalized._
import scala.reflect.ClassTag

class InlineWidgetWrapper(
    var index: Int,
    widget: Component,
    deleteCall: Int => Unit)
    extends BoxPanel(Orientation.Horizontal) {
  contents += widget
  contents += new Button(Action(needsTranslation("Delete")) {
    deleteCall(index)
  })
}

/**
 * Used to edit an array with a set of inline widgets. This differs from the
 * ArrayEditingPanel derived classes in a few ways:
 *  - The editing takes place in an in-line rich widget instead of in a separate
 *    pane or dialog box.
 *  - There is no explicit resizing of the array other than by adding or
 *    deleting specific elements.
 *  - This is used for arrays with a few elements rather than many.
 * @param     onUpdate    Called when the contents change.
 */
abstract class InlineWidgetArrayEditor[T: ClassTag](
  owner: Window,
  initial: Array[T],
  onUpdate: (Array[T]) => Unit)
  extends BoxPanel(Orientation.Vertical) {

  background = UIManager.getColor("TextArea.background")

  def addAction(index: Int)

  val model = ArrayBuffer(initial : _*)

  def newInlineWidget(elementModel: T): Component

  def newWrappedInlineWidget(index: Int, elementModel: T) = {
    new InlineWidgetWrapper(index, newInlineWidget(elementModel), deleteCmd)
  }

  for ((element, i) <- model.zipWithIndex) {
    contents += newWrappedInlineWidget(i, element)
  }

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      if (e.peer.getButton() == MouseEvent.BUTTON3) {
        val menu = new RpgPopupMenu {
          contents += new MenuItem(Action(needsTranslation("Add") + "...") {
            addAction(model.length)
          })
        }

        menu.show(this, e.point.x, e.point.y)
      } else if (e.clicks == 2) {
        addAction(model.length)
      }
  }

  def insertElement(index: Int, element: T) = {
    assert(index >= 0)
    assert(index <= model.length)

    model.insert(index, element)
    onUpdate(model.toArray)

    // Insert a new panel.
    contents.insert(index, newWrappedInlineWidget(index, element))
    // Update the index of all the event panels following this one.
    for (i <- (index + 1) until model.length) {
      contents(i).asInstanceOf[InlineWidgetWrapper].index += 1
    }
    revalidate()
  }

  def updateElement(index: Int, element: T): Unit = {
    model.update(index, element)
    onUpdate(model.toArray)

    // Insert a new panel.
    contents.update(index, newWrappedInlineWidget(index, element))
    revalidate()
  }

  def deleteCmd(index: Int) = {
    assert(index >= 0)
    assert(index < model.length)
    model.remove(index)
    onUpdate(model.toArray)

    contents.remove(index)
    for (i <- index until model.length) {
      contents(i).asInstanceOf[InlineWidgetWrapper].index -= 1
    }
    revalidate()
  }
}