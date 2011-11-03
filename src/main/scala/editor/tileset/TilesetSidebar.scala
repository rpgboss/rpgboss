package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import rpgboss.editor._

import java.awt.image.BufferedImage

class TilesetSidebar(sm: StateMaster)
extends BoxPanel(Orientation.Horizontal)
{
  val thisSidebar = this
  
  def defaultTileCodes = Array(Array(Array(
    RpgMap.autotileByte, 0.asInstanceOf[Byte], 0.asInstanceOf[Byte])))
  
  // This var must always have at least be 1x1.
  // array of row vectors, so selectedTileCodes(y)(x)
  var selectedTileCodes : Array[Array[Array[Byte]]] = defaultTileCodes
  
  def selectMap(mapOpt: Option[RpgMap]) = setContent(mapOpt map { map =>
    new TabbedPane() {
      tabPlacement(Alignment.Bottom)
      
      val autotileSel = new AutotileSelector(sm.proj, TilesetSidebar.this)
      pages += new TabbedPane.Page("Autotiles", autotileSel)
      
      map.tilesets.zipWithIndex.map({
        case (tsName, i) => 
          val tileset = Tileset.readFromDisk(sm.proj, tsName)
          val tabComponent = tileset.imageOpt.map { img =>
            new ImageTileSelector(img, tXYArray =>
              selectedTileCodes = tXYArray.map(_.map({
                case (xTile, yTile) => 
                  Array(i.asInstanceOf[Byte], xTile, yTile)
              }))
            )
          } getOrElse new Label("No image")
          
          pages += new TabbedPane.Page(tsName, tabComponent)
      })
      
      // select first Autotile code
      selectedTileCodes = defaultTileCodes
    }
  })
  
  def setContent(cOpt: Option[Component]) = {
    contents.clear()
    cOpt map { contents += _ }
    revalidate()
  }
}

