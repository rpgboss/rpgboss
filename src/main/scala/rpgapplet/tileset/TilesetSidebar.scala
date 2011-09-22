package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import rpgboss.rpgapplet._

import java.awt.image.BufferedImage

class TilesetSidebar(sm: StateMaster)
extends BoxPanel(Orientation.Vertical)
{
  def selectMap(map: RpgMap) = {
    val tilesetsPane = new TabbedPane() {
      tabPlacement(Alignment.Bottom)
      pages += new TabbedPane.Page("Autotiles", new AutotileSelector(sm.proj))
      
      
    }
    
    swapPanel.setContent(Some(tilesetsPane))
  }
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    val pencilBtn = new RadioButton("Pencil")
    val bGrp = new ButtonGroup(pencilBtn)
    
    contents += pencilBtn
    
    contents += new Button(Action("Tileset Properties") {
      
    })
  }
  
  val swapPanel = new BoxPanel(Orientation.Vertical) {
    def setContent(cOpt: Option[Component]) = {
      contents.clear()
      cOpt map { contents += _ }
      revalidate()
    }
  }
  
  contents += toolbar
  contents += swapPanel
}

