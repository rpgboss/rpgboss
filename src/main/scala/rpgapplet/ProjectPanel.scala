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
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    tilesetSidebar.selectMap(mapOpt)
    mapView.selectMap(mapOpt)
  }
  
  // select most recent or first map if not empty
  selectMap({
    if(!sm.maps.isEmpty) {
      val idToLoad =
        if(sm.maps.contains(sm.proj.recentMapId))
          sm.proj.recentMapId
        else
          sm.maps.keys.min
      
      sm.maps.get(idToLoad).map(_.mapMeta)
    }
    else None
  })
  
  mainP.revalidate()
}

