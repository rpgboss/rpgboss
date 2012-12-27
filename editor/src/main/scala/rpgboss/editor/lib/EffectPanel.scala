package rpgboss.editor.lib

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import com.weiglewilczek.slf4s.Logging
import java.awt.{Font, Color}
import javax.swing.table.AbstractTableModel
import rpgboss.model.Effect

class EffectPanel(
    owner: Window, 
    dbDiag: DatabaseDialog,
    initial: Array[Effect],
    onUpdate: Array[Effect] => Unit) 
  extends BoxPanel(Orientation.Vertical)
{
  border = BorderFactory.createTitledBorder("Effects")
  
  val effects = initial
  val table = new Table() {
    model = new AbstractTableModel() {
      val colNames = Array("Description", "Key", "Value")
      
      def getRowCount() = effects.size
      def getColumnCount() = 3
      override def getColumnName(col: Int) = colNames(col)
      
      def getValueAt(row: Int, col: Int) = {
        val eff = effects(row)
        col match {
          case 0 => ""
          case 1 => eff.key
          case 2 => eff.v.toString
        }
      }
      
      override def isCellEditable(r: Int, c: Int) = false
    } 
  }
  
  contents += new ScrollPane {
    contents = table
  }
}