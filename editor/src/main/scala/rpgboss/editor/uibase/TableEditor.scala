package rpgboss.editor.uibase

import javax.swing.table.AbstractTableModel
import scala.swing.Table
import scala.swing.event.MouseClicked
import java.awt.event.MouseEvent
import scala.swing.MenuItem
import scala.swing.Action

/**
 * A table that can be edited by context menu. Can add a new element by double
 * clicking the last empty row.
 */
abstract class TableEditor extends Table {
  def colHeaders: Array[String]
  def getRowStrings(row: Int): Array[String]
  def columnCount: Int
  def modelRowCount: Int
  
  def showEditDialog(row: Int, updateDisplayFunction: () => Unit)
  def showNewDialog(updateDisplayFunction: () => Unit)
  def deleteRow(row: Int, updateDisplayFunction: () => Unit)
  
  val tableModel = new AbstractTableModel() {
    def getRowCount() = modelRowCount + 1 // last element blank for adding
    def getColumnCount() = columnCount
    override def getColumnName(col: Int) = colHeaders(col)
  
    def getValueAt(row: Int, col: Int) = {
      // There should be just one extra blank row at the end of the table.
      assume(row < modelRowCount + 1)
      if (row < modelRowCount) {
        val rowStrings = getRowStrings(row)
        rowStrings(col)
      } else {
        "" // blank for new row
      }
    }
  
    override def isCellEditable(r: Int, c: Int) = false
  
  }
  
  model = tableModel

  selection.elementMode = Table.ElementMode.Row
  selection.intervalMode = Table.IntervalMode.Single

  // TODO: Refactor the duplicate tableModel.fire ... calls
  listenTo(mouse.clicks)
  reactions += {
    case MouseClicked(_, _, _, 2, _) => {
      val row = selection.rows.head
      if (row < modelRowCount) {
        showEditDialog(row, () => tableModel.fireTableRowsUpdated(row, row))
      } else {
        showNewDialog(() => {
          tableModel.fireTableRowsUpdated(modelRowCount - 1, modelRowCount - 1)
          tableModel.fireTableRowsInserted(modelRowCount, modelRowCount)
        })
      }
    }
    case e: MouseClicked if e.peer.getButton() == MouseEvent.BUTTON3 => {
      val (x0, y0) = (e.point.getX().toInt, e.point.getY().toInt)

      val row = peer.rowAtPoint(e.point)

      if (row != -1) {
        selection.rows.clear()
        selection.rows += row
        val menu = new RpgPopupMenu {
          contents += new MenuItem(Action("New...") {
            showNewDialog(() => {
              tableModel.fireTableRowsUpdated(modelRowCount - 1, 
                                              modelRowCount - 1)
              tableModel.fireTableRowsInserted(modelRowCount, modelRowCount)
            })
          })

          if (row != rowCount - 1) {
            contents += new MenuItem(Action("Edit...") {
              showEditDialog(row, 
                             () => tableModel.fireTableRowsUpdated(row, row))
            })
            contents += new MenuItem(Action("Delete") {
              deleteRow(row, () => tableModel.fireTableRowsDeleted(row, row))
            })
          }
        }
        menu.show(this, x0, y0)
      }
    }
  }

}