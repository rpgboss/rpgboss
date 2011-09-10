package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import java.awt.image.BufferedImage

class TilesetSidebar()
extends BoxPanel(Orientation.Vertical)
{
  var tilesetOpt : Option[(Int, Tileset)] = None
  var tileSelection: Option[(Int, Int)] = None
  
  val selectBox = new ComboBox(List.empty[(Int, Tileset)]) {
    import scala.swing.ListView._
    renderer = Renderer(_._2.name.rName)
  }
  
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    val pencilBtn = new RadioButton("Pencil")
    val bGrp = new ButtonGroup(pencilBtn)
    
    contents += pencilBtn
    
    contents += new Button(Action("Tileset Properties") {
      
    })
  }
  
  val selectorContainer = new BoxPanel(Orientation.Vertical) {
    def setContent(cOpt: Option[Component]) = {
      contents.clear()
      cOpt map { contents += _ }
      revalidate()
    }
  }
  
  contents += selectBox
  contents += toolbar
  contents += selectorContainer
  
  def setTilesets(numberedTilesets : List[(Int, Tileset)], sel: Some[Int]) = {
    if(!numberedTilesets.isEmpty) {
      // update combobox
      selectBox.peer.setModel(ComboBox.newConstantModel(numberedTilesets))
      
      // clear selector container
      selectorContainer.setContent(None)
      
      // Update combobox selection
      sel map { s =>
        val selIdx = numberedTilesets.indexWhere(_._1 == s)
        if(selIdx != -1) selectBox.selection.index = selIdx
      }
    }
  }
  
  listenTo(selectBox.selection)
  
  reactions += {
    case SelectionChanged(`selectBox`) => {
      val (tsNum, tileset) = selectBox.selection.item
      tilesetOpt = Some(selectBox.selection.item)
      
      val selector = new ImageTileSelector(tileset.getImage,
                                           sel => tileSelection = Some(sel),
                                           Tileset.tilesize,
                                           Tileset.tilesize,
                                           8)
      
      selectorContainer.setContent(Some(selector))
    }
  }
}

