package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import rpgboss.lib._
import javax.swing.BorderFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.awt.{ Font, Color }
import scala.swing.ListView.Renderer
import scala.collection.mutable.ArrayBuffer

class ArrayListView[T](initialAry: Seq[T]) extends ListView(initialAry) {

  def onListSelectionChanged(): Unit = {}
  def label(a: T): String = a.toString

  renderer = standardIdxRenderer(label _)

  listenTo(selection)
  reactions += {
    case ListSelectionChanged(_, _, _) => onListSelectionChanged()
  }
}

abstract class ArrayEditingPanel[T <: AnyRef](
  owner: Window,
  label: String,
  initialAry: Seq[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends DesignGridPanel
  with LazyLogging {
  def newDefaultInstance(): T
  def label(a: T): String

  val editPaneContainer = new BoxPanel(Orientation.Vertical)
  def editPaneForItem(idx: Int, item: T): Component
  def editPaneEmpty: Component

  def dataAsArray : Array[T] = {
    listView.listData.toArray
  }

  // Just refresh the label of the item on the list
  def refreshModel() = updatePreserveSelection(listView.listData)

  def updatePreserveSelection(idx: Int, newVal: T): Unit =
    updatePreserveSelection(listView.listData.updated(idx, newVal))

  def updatePreserveSelection(newData: Seq[T]) = {
    listView.deafTo(listView.selection)

    if (listView.selection.indices.isEmpty) {
      listView.listData = newData
    } else {
      val oldSelection = listView.selection.indices.head
      listView.listData = newData
      listView.selectIndices(oldSelection)
    }

    listView.listenTo(listView.selection)
    onListDataUpdate()
  }

  def onListDataUpdate() = {
    logger.info("Empty list update call")
  }

  def normalizedInitialAry =
    ArrayUtils.normalizedAry(
      initialAry, minElems, maxElems, newDefaultInstance _)

  val listView = new ArrayListView(normalizedInitialAry) {
    override def label(a: T) = ArrayEditingPanel.this.label(a)

    override def onListSelectionChanged() = {
      editPaneContainer.contents.clear()

      if (selection.indices.isEmpty) {
        editPaneContainer.contents += editPaneEmpty
      } else {
        val editPane = editPaneForItem(
          selection.indices.head,
          selection.items.head)

        editPaneContainer.contents += editPane
      }

      editPaneContainer.revalidate()
      editPaneContainer.repaint()
    }
  }

  val btnSetListSize = new Button(Action("Set array size...") {
    val dialog = new ArraySizeDialog(
      owner,
      listView.listData.size,
      minElems,
      maxElems,
      (newSize) => {
        if (listView.selection.indices.isEmpty) {
          listView.listData = ArrayUtils.resized(
              listView.listData, newSize, newDefaultInstance _)
        } else {
          val oldSelection = listView.selection.indices.head

          listView.listData = ArrayUtils.resized(
              listView.listData, newSize, newDefaultInstance _)

          // If selection was bigger than the size, select the last one
          listView.selectIndices(math.min(oldSelection, newSize - 1))
        }
        onListDataUpdate()
      })
    dialog.open()
  })

  val btnDuplicateItem = new Button(Action("Duplicate item...") {
    if (listView.selection.indices.isEmpty) {
      SwingUtils.showErrorDialog(this, "No item selected.")
    } else {
      val originalItemIdx = listView.selection.indices.head
      val availableSlots = listView.listData.size - originalItemIdx - 1
      if (availableSlots < 1) {
        SwingUtils.showErrorDialog(this, "No slots to duplicate into.")
      } else {
        val origItem: T = listView.selection.items.head
        val dialog = new ArrayDuplicateDialog(
          owner,
          availableSlots,
          slotsToCopy => {
            val buf = listView.listData.toBuffer
            for (i <- originalItemIdx to (originalItemIdx + slotsToCopy)) {
              buf.update(i, Utils.deepCopy(origItem))
            }
            listView.listData = buf
            onListDataUpdate()
          })
        dialog.open()
      }
    }
  })

  editPaneContainer.contents += editPaneEmpty
}

class ArraySizeDialog(
  owner: Window,
  initial: Int,
  min: Int,
  max: Int,
  okCallback: Int => Unit)
  extends SingleIntegerDialog(
    owner,
    "Set array size",
    "Array size:",
    "",
    initial,
    min,
    max,
    okCallback)

class ArrayDuplicateDialog(
  owner: Window,
  maxDuplicates: Int,
  okCallback: Int => Unit)
  extends SingleIntegerDialog(
    owner,
    "Duplicate item",
    "No. of duplicates:",
    "Duplicates are copied to slots below item.",
    1,
    1,
    maxDuplicates,
    okCallback)

class StringArrayEditingPanel(
  owner: Window,
  label: String,
  initialAry: Seq[String],
  minElems: Int = 1,
  maxElems: Int = 1024)
  extends ArrayEditingPanel(owner, label, initialAry, minElems, maxElems) {
  def newDefaultInstance() = ""
  def label(a: String) = a

  def editPaneForItem(idx: Int, item: String) = {
    new TextField {
      text = item

      reactions += {
        case ValueChanged(_) =>
          updatePreserveSelection(idx, text)
      }
    }
  }
  def editPaneEmpty = new TextField {
    enabled = false
    text = "Select an item to edit"
  }

  val scrollPane = new ScrollPane {
    contents = listView
  }

  border = BorderFactory.createTitledBorder(label)

  row().grid().add(editPaneContainer)
  row().grid().add(scrollPane)
  row().grid().add(btnSetListSize)
}

abstract class RightPaneArrayEditingPanel[T <: AnyRef](
  owner: Window,
  label: String,
  initialAry: Seq[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends ArrayEditingPanel[T](
    owner,
    label,
    initialAry,
    minElems,
    maxElems)(m) {
  def editPaneEmpty = new BoxPanel(Orientation.Vertical)

  val bigLbl = new Label {
    text = label
    font = new Font("Arial", Font.BOLD, 14)
    horizontalAlignment = Alignment.Center
    background = Color.BLACK
    opaque = true
    foreground = Color.WHITE
  }

  val scrollPane = new ScrollPane {
    contents = listView
  }

  row().grid().add(new BoxPanel(Orientation.Horizontal) {
    contents += new DesignGridPanel {
      maximumSize = new Dimension(250, 5000)
      preferredSize = new Dimension(250, 500)
      minimumSize = new Dimension(250, 250)

      row().grid().add(bigLbl)
      row().grid().add(scrollPane)
      row().grid().add(btnSetListSize)
      row().grid().add(btnDuplicateItem)
    }

    contents += (editPaneContainer)
  })

  // Lazily select the first index both for performance, and also so that we
  // don't cause graphics artifacts from drawing OpenGL stuff to the canvas.
  // Specifically, this is to fix the bug with the Battlers field being painted
  // over the other tabs.
  listenTo(this)
  reactions += {
    case UIElementShown(_) => {
      listView.selectIndices(0)
    }
  }
}