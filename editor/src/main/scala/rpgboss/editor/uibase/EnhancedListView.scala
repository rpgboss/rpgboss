package rpgboss.editor.uibase

import scala.swing.ListView
import com.typesafe.scalalogging.slf4j.LazyLogging

trait HasEnhancedListView[T] extends LazyLogging {
  def listView: ListView[T]

  def updatePreserveSelection(idx: Int, newVal: T): Unit = {
    listView.deafTo(listView.selection)
    val oldIdx = listView.selection.indices.head
    listView.listData = listView.listData.updated(idx, newVal)
    listView.selectIndices(oldIdx)
    listView.listenTo(listView.selection)
    onListDataUpdate()
  }

  def updatePreserveSelection(newData: Seq[T]) = {
    listView.deafTo(listView.selection)

    if (listView.selection.indices.isEmpty) {
      listView.listData = newData
    } else {
      val oldSelectionObject = listView.selection.items.head
      val oldSelectionIdx = listView.selection.indices.head

      val newIdx = {
        val valueEqualIdx = newData.indexWhere(_ == oldSelectionObject)

        if (valueEqualIdx == -1) {
          math.min(oldSelectionIdx, newData.length - 1)
        } else {
          valueEqualIdx
        }
      }

      listView.listData = newData
      listView.selectIndices(newIdx)
    }

    listView.listenTo(listView.selection)
    onListDataUpdate()
  }

  def onListDataUpdate() = {
    logger.info("Empty list update call")
  }
}

trait IsEnhancedListView[T] extends ListView[T] with HasEnhancedListView[T] {
  def listView = this
}