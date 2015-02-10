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
import javax.swing.border.BevelBorder
import rpgboss.editor.util.MouseUtil

class InlineWidgetWrapper(
    parent: InlineWidgetArrayEditor[_],
    var index: Int,
    widget: Component)
    extends BoxPanel(Orientation.Horizontal) {
  border = BorderFactory.createBevelBorder(BevelBorder.RAISED)

  val deleteButton = new Button(Action(getMessage("Delete")) {
    parent.deleteElement(index)
  })
  contents += widget
  contents += deleteButton

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      requestFocus()
      if (MouseUtil.isRightClick(e)) {
        val menu = new RpgPopupMenu {
          contents += new MenuItem(Action(getMessage("Insert_Above") + "...") {
            parent.addAction(index)
          })
          parent.genericEditAction.map { editAction =>
            contents += new MenuItem(Action(getMessage("Edit") + "...") {
              editAction(index)
            })
          }
          contents += new MenuItem(Action(getMessage("Delete")) {
            parent.deleteElement(index)
          })
        }

        menu.show(this, e.point.x, e.point.y)
      } else if (e.clicks == 2) {
        parent.genericEditAction.map(editAction => editAction(index))
      }
  }
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

  def title: String
  def addAction(index: Int)
  def newInlineWidget(elementModel: T): Component

  def genericEditAction: Option[Int => Unit] = None

  def getAddPanel(): Option[Component] = {
    val panel = new BoxPanel(Orientation.Horizontal) {
      contents += Swing.HGlue
      contents += new Button(Action(getMessage("Add") + "...") {
        addAction(model.length)
      })
      contents += Swing.HGlue
    }
    Some(panel)
  }

  border = BorderFactory.createTitledBorder(title)

  val model = ArrayBuffer(initial : _*)
  def sendUpdate() = onUpdate(model.toArray)

  def newWrappedInlineWidget(index: Int, elementModel: T) = {
    new InlineWidgetWrapper(this, index, newInlineWidget(elementModel))
  }

  val arrayPanel = new BoxPanel(Orientation.Vertical) {
    background = UIManager.getColor("TextArea.background")

    listenTo(mouse.clicks)
    reactions += {
      case e: MouseClicked =>
        if (MouseUtil.isRightClick(e)) {
          val menu = new RpgPopupMenu {
            contents +=
              new MenuItem(Action(getMessage("Add") + "...") {
                addAction(model.length)
              })
          }

          menu.show(this, e.point.x, e.point.y)
        } else if (e.clicks == 2) {
          addAction(model.length)
        }
    }
  }

  for ((element, i) <- model.zipWithIndex) {
    arrayPanel.contents += newWrappedInlineWidget(i, element)
  }
  getAddPanel.map(arrayPanel.contents += _)
  arrayPanel.contents += Swing.VGlue

  def preferredWidth = 200
  val scrollPane = new ScrollPane {
    preferredSize = new Dimension(preferredWidth, 200)
    contents = arrayPanel
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
    verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  }

  contents += scrollPane

  def revalidateAndRepaint() = {
    scrollPane.revalidate()
    scrollPane.repaint()
  }

  def insertElement(index: Int, element: T) = {
    assert(index >= 0)
    assert(index <= model.length)

    model.insert(index, element)
    sendUpdate()

    // Insert a new panel.
    arrayPanel.contents.insert(index, newWrappedInlineWidget(index, element))
    // Update the index of all the event panels following this one.
    for (i <- (index + 1) until model.length) {
      arrayPanel.contents(i).asInstanceOf[InlineWidgetWrapper].index += 1
    }
    revalidateAndRepaint()
  }

  def deleteElement(index: Int) = {
    assert(index >= 0)
    assert(index < model.length)
    model.remove(index)
    sendUpdate()

    arrayPanel.contents.remove(index)
    for (i <- index until model.length) {
      arrayPanel.contents(i).asInstanceOf[InlineWidgetWrapper].index -= 1
    }
    revalidateAndRepaint()
  }
}