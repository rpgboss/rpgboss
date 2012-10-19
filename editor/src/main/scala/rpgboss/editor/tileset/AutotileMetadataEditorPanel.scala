package rpgboss.editor.tileset

import scala.swing._
import rpgboss.editor.lib.SwingUtils._
import scala.swing.event._
import rpgboss.editor.lib._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster

class AutotileMetadataEditorPanel(sm: StateMaster) 
  extends BoxPanel(Orientation.Horizontal) {
  
  val autotiles = 
    Autotile.list(sm.getProj).map(Autotile.readFromDisk(sm.getProj, _))
  
  def clickAutotile(idx: Int) = {}
  
  val leftPanel = new DesignGridPanel {
    row.grid.add(leftLabel("Autotiles"))
    row.grid.add()
  }
  
  val rightPanel = new BoxPanel(Orientation.Vertical)
  
  contents += leftPanel
  contents += rightPanel
}
