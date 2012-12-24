package rpgboss.editor.lib

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import com.weiglewilczek.slf4s.Logging
import java.awt.{Font, Color}

object ArrayEditingPanel {
  import ListView._
  
  // Renders the item with the index passed in
  def idxRenderer[A,B](f: (A, Int) => B)(implicit renderer: Renderer[B]) = 
    new Renderer[A] {
	  def componentFor(
	      list: ListView[_], 
	      isSelected: Boolean, 
	      focused: Boolean, 
	      a: A, 
	      index: Int): Component =
        renderer.componentFor(list, isSelected, focused, f(a, index), index)
    }
}

abstract class ArrayEditingPanel[T](
    owner: Window,
    label: String,
    initialAry: Array[T],
    minElems: Int = 1,
    maxElems: Int = 1024)(implicit m: Manifest[T]) 
  extends DesignGridPanel
  with Logging
{
  def newDefaultInstance(): T
  def label(a: T): String
  
  val editPaneContainer = new BoxPanel(Orientation.Vertical)
  def editPaneForItem(idx: Int, item: T): Component
  def editPaneEmpty: Component
  
  def array = listView.listData.toArray
  
  def resized(a: Seq[T], newSize: Int) = {
    val oldSize = a.size
    
    if(newSize > oldSize) {
      val padder = Array.fill(newSize-oldSize){ newDefaultInstance() }
      a ++ padder
    } else if(newSize < oldSize){
      a.take(newSize)
    } else a
  }
  
  def updatePreserveSelection(idx: Int, newVal: T) = {
    deafTo(listView.selection)
    
    val newData = listView.listData.updated(idx, newVal)
    
    if(listView.selection.indices.isEmpty) {
      listView.listData = newData
    } else {
      val oldSelection = listView.selection.indices.head 
      listView.listData = newData
      listView.selectIndices(oldSelection)
    }
    
    listenTo(listView.selection)
  }
  
  def normalizedInitialAry = 
    if(initialAry.size > maxElems)
      resized(initialAry, maxElems).toArray
    else if(initialAry.size < minElems)
      resized(initialAry, minElems).toArray
    else
      initialAry
  
  val listView = new ListView(normalizedInitialAry) {
    renderer = ArrayEditingPanel.idxRenderer({ case (a, idx) =>
      "%d: %s".format(idx, label(a))
    })
  }
  
  val btnSetListSize = new Button(Action("Set array size...") {
    val diag = 
      new ArraySizeDiag(
          owner, 
          listView.listData.size, 
          minElems, 
          maxElems, 
          (newSize) => {
            if(listView.selection.indices.isEmpty) {
              listView.listData = resized(listView.listData, newSize)
            } else {
              val oldSelection = listView.selection.indices.head
              
              listView.listData = resized(listView.listData, newSize)
              
              // If selection was bigger than the size, select the last one
              listView.selectIndices(math.min(oldSelection, newSize-1))
            }
          })
    diag.open()
  })
  
  listenTo(listView.selection)
  reactions += {
    case ListSelectionChanged(`listView`, _, _) =>
      editPaneContainer.contents.clear()
      
      if(listView.selection.indices.isEmpty) {
        editPaneContainer.contents += editPaneEmpty
      } else {
        val editPane = editPaneForItem(
            listView.selection.indices.head,
            listView.selection.items.head)
        
        editPaneContainer.contents += editPane
      }
      
      editPaneContainer.revalidate()
      editPaneContainer.repaint()
  }
  
  editPaneContainer.contents += editPaneEmpty
}

class ArraySizeDiag(
    owner: Window, 
    initial: Int,
    min: Int,
    max: Int,
    okCallback: Int => Unit) 
  extends StdDialog(owner, "Set array size") 
{
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
    initialAry: Array[String],
    minElems: Int = 1,
    maxElems: Int = 1024)
  extends ArrayEditingPanel(owner, label, initialAry, minElems, maxElems) 
{
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
  
  border = BorderFactory.createTitledBorder(label)
  
  row().grid().add(editPaneContainer)
  row().grid().add(listView)
  row().grid().add(btnSetListSize)
}

abstract class RightPaneArrayEditingPanel[T](
    owner: Window,
    label: String,
    initialAry: Array[T],
    minElems: Int = 1,
    maxElems: Int = 1024)(implicit m: Manifest[T]) 
  extends ArrayEditingPanel[T](owner, label, initialAry, minElems, maxElems)(m)
{
  def editPaneEmpty = new BoxPanel(Orientation.Vertical)
  
  val bigLbl = new Label {
    text = label
    font = new Font("Arial", Font.BOLD, 14)
    horizontalAlignment = Alignment.Center
    background = Color.BLACK
    opaque = true
    foreground = Color.WHITE
  } 
  
  row().grid().add(new BoxPanel(Orientation.Horizontal) {
    contents += new DesignGridPanel {
      row().grid().add(bigLbl)
      row().grid().add(listView)
      row().grid().add(btnSetListSize)
    }
    
    contents += (editPaneContainer)
  })
  
  listView.selectIndices(0)
}