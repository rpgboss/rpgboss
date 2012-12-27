package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.editor.dialog._

import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class EquipmentPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends RightPaneArrayEditingPanel(
      owner, 
      "Equipment", 
      dbDiag.model.equipment)
  with DatabasePanel
{
  def panelName = "Equipment"
  def newDefaultInstance() = new Item()
  def label(item: Item) = item.name
  
  def editPaneForItem(idx: Int, initial: Item) = {
    var model = initial
      
    def updateModel(newModel: Item) = {
      model = newModel
      updatePreserveSelection(idx, model)
    }

    new BoxPanel(Orientation.Horizontal) {
    }
  }
  
  
  override def onListDataUpdate() = {
    logger.info("Equipment data updated")
    dbDiag.model = dbDiag.model.copy(
        equipment = array
    )
  }
}