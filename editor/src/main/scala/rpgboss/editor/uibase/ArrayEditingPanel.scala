package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import rpgboss.lib._
import javax.swing.BorderFactory
import com.typesafe.scalalogging.slf4j.Logging
import java.awt.{ Font, Color }
import scala.swing.ListView.Renderer
import scala.collection.mutable.ArrayBuffer

class ArrayListView[T](initialAry: Seq[T]) extends ListView(initialAry) {
  
  def onListSelectionChanged(): Unit = {}
  def label(a: T): String = a.toString
  
  // Renders the item with the index passed in
  def idxRenderer[A, B](f: (A, Int) => B)(implicit renderer: Renderer[B]) =
    new Renderer[A] {
      def componentFor(
        list: ListView[_],
        isSelected: Boolean,
        focused: Boolean,
        a: A,
        index: Int): Component =
        renderer.componentFor(list, isSelected, focused, f(a, index), index)
    }
  
  renderer = idxRenderer({
    case (a, idx) =>
      "%d: %s".format(idx, label(a))
  })

  listenTo(selection)
  reactions += {
    case ListSelectionChanged(_, _, _) => onListSelectionChanged()
  }
}

abstract class ArrayEditingPanel[T](
  owner: Window,
  label: String,
  initialAry: Seq[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends DesignGridPanel
  with Logging {
  def newDefaultInstance(): T
  def label(a: T): String

  val editPaneContainer = new BoxPanel(Orientation.Vertical)
  def editPaneForItem(idx: Int, item: T): Component
  def editPaneEmpty: Component

  def arrayBuffer : ArrayBuffer[T] = {
    val b = new ArrayBuffer[T]
    listView.listData.copyToBuffer(b)
    b
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
    val diag =
      new ArraySizeDiag(
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
    diag.open()
  })

  editPaneContainer.contents += editPaneEmpty
}

class ArraySizeDiag(
  owner: Window,
  initial: Int,
  min: Int,
  max: Int,
  okCallback: Int => Unit)
  extends StdDialog(owner, "Set array size") {
  val fieldSize = new NumberSpinner(initial, min, max)

  def okFunc() = {
    okCallback(fieldSize.getValue)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Array size:"))
    row().grid().add(fieldSize)
    addButtons(cancelBtn, okBtn)
  }
}

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

abstract class RightPaneArrayEditingPanel[T](
  owner: Window,
  label: String,
  initialAry: Seq[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends ArrayEditingPanel[T](owner, label, initialAry, minElems, maxElems)(m) {
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
      row().grid().add(bigLbl)
      row().grid().add(scrollPane)
      row().grid().add(btnSetListSize)
    }

    contents += (editPaneContainer)
  })

  listView.selectIndices(0)
}