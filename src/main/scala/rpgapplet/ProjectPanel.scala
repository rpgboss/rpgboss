package rpgboss.rpgapplet

import rpgboss.rpgapplet.dialog._
import rpgboss.rpgapplet.lib._
import rpgboss.rpgapplet.tileset._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class ProjectPanel(mainP: MainPanel, project: Project)
  extends SplitPane(Orientation.Vertical)
{
  val sm = new StateMaster(project)
  
  val tilesetSidebar = new TilesetSidebar(sm)
  val mapSelector = new MapSelector(sm, this)
  val mapView = new MapView(sm, tilesetSidebar)
  
  topComponent =
    new SplitPane(Orientation.Horizontal, tilesetSidebar, mapSelector)
  bottomComponent = mapView
  enabled = false
  
  def selectMap(map: RpgMap) = {
    tilesetSidebar.selectMap(map)
    mapView.selectMap(map)
  }
  
  // select most recent or first map if not empty
  if(!sm.maps.isEmpty) 
  {
    val recentMap = 
      sm.maps.find(_.id == sm.proj.recentMapId) getOrElse sm.maps.head
    selectMap(recentMap)
  }
  
  mainP.revalidate()
}

