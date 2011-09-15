package rpgboss.rpgapplet.dialog

import rpgboss.rpgapplet.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.model._

import rpgboss.message.Messages._

import net.java.dev.designgridlayout._

import java.io.File

class LoadProjectDialog(owner: Window, onSuccess: Project => Any) 
  extends StdDialog(owner, "Load Project")
{ 
  val rootChooser = Paths.getRootChooserPanel(() => populateList())
  
  val projList = new Table() {
    selection.intervalMode = Table.IntervalMode.Single
    selection.elementMode = Table.ElementMode.Row
  }
  
  def populateList() : Unit = {
    val projects : Array[ProjectMetadata] = {
      val rootPath = rootChooser.getRoot
      if(rootPath.isDirectory && rootPath.canRead) 
      {
        val projMetadatas : Array[Option[ProjectMetadata]] = 
          rootPath.listFiles.map( child => {
            if(child.isDirectory && child.canRead) 
              ProjectMetadata.readFromDisk(child)
            else None
          })
        
        projMetadatas.flatten
      }
      else Array.empty
    }
    
    val tableModel = new javax.swing.table.AbstractTableModel() {
      def getColumnCount = 2
      def getRowCount = projects.length
      
      val cols = Array("Shortname", "Title")
      
      override def getColumnName(col: Int) = cols(col)
      
      def getValueAt(r: Int, c: Int) = c match {
        case 0 => projects(r).shortName
        case _ => projects(r).title
      }
      
      override def isCellEditable(r: Int, c: Int) = false
    } 
    
    projList.model = tableModel
    
    projList.peer.getColumnModel().getColumn(0).setPreferredWidth(50)
    projList.peer.getColumnModel().getColumn(1).setPreferredWidth(100)  
  }
  
  populateList()
  
  def okFunc() = {
    
  }
  
  
  contents = new DesignGridPanel {
    
    row().grid().add(leftLabel("Directory for all projects:"))
    row().grid().add(rootChooser)
    
    row().grid().add(leftLabel("Projects:"))
    row().grid().add(new ScrollPane {
      preferredSize = new Dimension(300, 150)
      contents = projList
    })
      
    addButtons(cancelButton, okButton)
  }
}