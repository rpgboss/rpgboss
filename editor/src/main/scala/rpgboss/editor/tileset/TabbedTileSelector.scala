package rpgboss.editor.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import rpgboss.editor._
import rpgboss.editor.lib._

import java.awt.image.BufferedImage

class TabbedTileSelector(sm: StateMaster)
extends BoxPanel(Orientation.Horizontal) with SelectsMap
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
      
      pages += new TabbedPane.Page("A", 
        new AutotileSelector(sm, map, TabbedTileSelector.this))
      
      map.metadata.tilesets.zipWithIndex.map({
        case (tsName, i) => 
          val tileset = sm.assetCache.tilesetMap(tsName)
          val tabComponent = new ImageTileSelector(tileset.img, tXYArray =>
            selectedTileCodes = tXYArray.map(_.map({
              case (xTile, yTile) => 
                Array(i.asInstanceOf[Byte], xTile, yTile)
            })))
          
          pages += new TabbedPane.Page("T%d".format(i), tabComponent)
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

