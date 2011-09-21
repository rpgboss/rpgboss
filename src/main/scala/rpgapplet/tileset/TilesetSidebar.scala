package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

class TilesetSidebar()
extends BoxPanel(Orientation.Vertical)
{
  var mapsToTabbedPanes : Map[Int, TabbedPane] = Map()
  
  def selectMap(map: RpgMap) = {
    
  }
  
  def createMapTab(map: RpgMap) = new TabbedPane() {
    tabPlacement(Alignment.Bottom)
    pages += new Page("Autotiles", new AutotileSelector(proj))
    
    
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

