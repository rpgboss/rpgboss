package rpgboss.editor.uibase

import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.model._
import javax.swing.BorderFactory
import com.typesafe.scalalogging.slf4j.Logging
import scala.swing.ListView.Renderer
import javax.swing.DefaultListSelectionModel

class ArrayMultiselectPanel[T <: HasName](
  owner: Window,
  label: String,
  choices: Seq[T],
  initialSelections: Seq[Int],
  onUpdate: Seq[Int] => Unit)
  extends DesignGridPanel
  with Logging {
  val listView = new ListView(choices) {
    renderer = new Renderer[T] {
      def componentFor(
        list: ListView[_],
        isSelected: Boolean,
        focused: Boolean,
        a: T,
        index: Int): Component =
        {
          new CheckBox(a.name) {
            selected = isSelected
          }
        }
    }

    selection.intervalMode = ListView.IntervalMode.MultiInterval

    // Make initial selections
    selectIndices(initialSelections.filter(_ >= 0): _*)

    // Respond to clicks appropriately: 
    // http://stackoverflow.com/questions/2528344/jlist-deselect-when-clicking-an-already-selected-item

    // HACK to get old listeners. May crash if implementing class changes from
    // a DefaultListSelectionModel
    val oldListeners = {
      val oldModel =
        peer.getSelectionModel().asInstanceOf[DefaultListSelectionModel]
      oldModel.getListSelectionListeners()
    }

    val newSelectionModel = new DefaultListSelectionModel() {
      var gestureStarted = false

      override def setSelectionInterval(i0: Int, i1: Int) = {
        if (!gestureStarted) {
          if (isSelectedIndex(i0)) {
            removeSelectionInterval(i0, i1)
          } else {
            addSelectionInterval(i0, i1)
          }
        }
        gestureStarted = true
      }

      override def setValueIsAdjusting(isAdjusting: Boolean) = {
        if (isAdjusting == false) {
          gestureStarted = false
        }
      }
    }

    // HACK to restore the listeners
    for (l <- oldListeners)
      newSelectionModel.addListSelectionListener(l)

    peer.setSelectionModel(newSelectionModel)

    def updateModel() = {
      val ary = selection.indices.toSeq
      onUpdate(ary)
      logger.info("Updated ArrayMultiselect: %s".format(ary))
    }

    // Call on update at approriate time
    listenTo(selection)
    reactions += {
      case ListSelectionChanged(_, _, _) => updateModel()
    }

  }

  border = BorderFactory.createTitledBorder(label)

  val scrollPane = new ScrollPane {
    contents = listView
  }

  row().grid().add(scrollPane)

}