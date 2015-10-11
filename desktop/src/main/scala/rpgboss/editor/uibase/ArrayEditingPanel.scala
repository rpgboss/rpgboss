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
import com.badlogic.gdx.utils.Disposable
import rpgboss.editor.Internationalized._

trait DisposableComponent extends Component with Disposable {
  def dispose() = {}
}

class ArrayListView[T](initialAry: Array[T]) extends ListView(initialAry) {

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
  initialAry: Array[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends DesignGridPanel
  with HasEnhancedListView[T]
  with Disposable {
  def newDefaultInstance(): T
  def label(a: T): String

  var currentEditPane: Option[DisposableComponent] = None
  def dispose() = currentEditPane.map(_.dispose())

  val editPaneContainer = new BoxPanel(Orientation.Vertical)
  def editPaneForItem(idx: Int, item: T): DisposableComponent
  def editPaneEmpty: DisposableComponent

  def dataAsArray : Array[T] = {
    listView.listData.toArray
  }

  // Just refresh the label of the item on the list
  def refreshModel() = updatePreserveSelection(listView.listData)

  def normalizedInitialAry =
    ArrayUtils.normalizedAry(
      initialAry, minElems, maxElems, newDefaultInstance _)

  val listView = new ArrayListView(normalizedInitialAry) {
    override def label(a: T) = ArrayEditingPanel.this.label(a)

    override def onListSelectionChanged() = {
      currentEditPane.map(_.dispose())
      editPaneContainer.contents.clear()

      val editPane = if (selection.indices.isEmpty) {
        editPaneEmpty
      } else {
        editPaneForItem(selection.indices.head, selection.items.head)
      }
      editPaneContainer.contents += editPane

      currentEditPane = Some(editPane)

      editPaneContainer.revalidate()
      editPaneContainer.repaint()

      editPane.revalidate()
      editPane.repaint()
    }
  }

  val btnSetListSize = new Button(Action(getMessage("Set_Array_Size")) {
    val dialog = new ArraySizeDialog(
      owner,
      listView.listData.size,
      minElems,
      maxElems,
      (newSize) => {
        if (listView.selection.indices.isEmpty) {
          listView.listData = ArrayUtils.resized(
              listView.listData.toArray, newSize, newDefaultInstance _)
        } else {
          val oldSelection = listView.selection.indices.head

          listView.listData = ArrayUtils.resized(
              listView.listData.toArray, newSize, newDefaultInstance _)

          // If selection was bigger than the size, select the last one
          listView.selectIndices(math.min(oldSelection, newSize - 1))
        }
        onListDataUpdate()
      })
    dialog.open()
  })

  val btnDuplicateItem = new Button(Action(getMessage("Duplicate_Item")) {
    if (listView.selection.indices.isEmpty) {
      SwingUtils.showErrorDialog(this, getMessage("No_Item_Selected"))
    } else {
      val originalItemIdx = listView.selection.indices.head
      val availableSlots = listView.listData.size - originalItemIdx - 1
      if (availableSlots < 1) {
        SwingUtils.showErrorDialog(this, getMessage("No_Slots_To_Duplicate_Into"))
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
    getMessage("Set_Array_Size"),
    getMessageColon("Array_Size"),
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
    getMessage("Duplicate_Item"),
    getMessageColon("No_Of_Duplicates"),
    getMessage("Duplicates_Are_Copied_To_Slots_Below_Item"),
    1,
    1,
    maxDuplicates,
    okCallback)

class StringArrayEditingPanel(
  owner: Window,
  arrayLabel: String,
  initial: Array[String],
  updateF: Array[String] => Unit,
  minElems: Int = 1,
  maxElems: Int = 1024)
  extends ArrayEditingPanel(owner, initial, minElems, maxElems) {
  def newDefaultInstance() = ""
  def label(a: String) = a

  override def onListDataUpdate() = updateF(dataAsArray)

  def editPaneForItem(idx: Int, item: String) = {
    new TextField with DisposableComponent {
      text = item

      reactions += {
        case ValueChanged(_) =>
          updatePreserveSelection(idx, text)
      }
    }
  }
  def editPaneEmpty = new TextField with DisposableComponent {
    enabled = false
    text = getMessage("Select_An_Item_To_Edit")
  }

  val scrollPane = new ScrollPane {
    contents = listView
  }

  border = BorderFactory.createTitledBorder(arrayLabel)

  row().grid().add(editPaneContainer)
  row().grid().add(scrollPane)
  row().grid().add(btnSetListSize)
}

abstract class RightPaneArrayEditingPanel[T <: AnyRef](
  owner: Window,
  initialAry: Array[T],
  minElems: Int = 1,
  maxElems: Int = 1024)(implicit m: Manifest[T])
  extends ArrayEditingPanel[T](
    owner,
    initialAry,
    minElems,
    maxElems)(m) {
  def arrayLabel: String
  def editPaneEmpty =
    new BoxPanel(Orientation.Vertical) with DisposableComponent

  val bigLbl = new Label {
    text = arrayLabel
    font = new Font("Arial", Font.BOLD, 14)
    horizontalAlignment = Alignment.Center
    background = Color.BLACK
    opaque = true
    foreground = Color.WHITE
  }

  val scrollPane = new ScrollPane {
    contents = listView
  }

  val editPaneScroll = new ScrollPane {
    preferredSize = new Dimension(400, 400)
    contents = editPaneContainer
  }

  row().grid().add(new BoxPanel(Orientation.Horizontal) {
    contents += new DesignGridPanel {
      maximumSize = new Dimension(250, 5000)
      preferredSize = new Dimension(250, 300)
      minimumSize = new Dimension(250, 150)

      row().grid().add(bigLbl)
      row().grid().add(scrollPane)
      row().grid().add(btnSetListSize)
      row().grid().add(btnDuplicateItem)
    }

    contents += editPaneScroll
  })

  // Lazily select the first index both for performance, and also so that we
  // don't cause graphics artifacts from drawing OpenGL stuff to the canvas.
  // Specifically, this is to fix the bug with the Battlers field being painted
  // over the other tabs.
  listenTo(this)
  // Event seems to be published twice.
  private var uiElementShownAlready = false
  reactions += {
    case UIElementShown(_) if !uiElementShownAlready => {
      listView.selectIndices(0)
      uiElementShownAlready = true
    }
  }
}