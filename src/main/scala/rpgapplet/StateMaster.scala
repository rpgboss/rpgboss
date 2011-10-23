package rpgboss.rpgapplet

import rpgboss.rpgapplet.lib._
import rpgboss.rpgapplet.tileset._

import rpgboss.model._
import rpgboss.message._

import scala.swing._

object Dirtiness extends Enumeration {
  val Clean, Dirty, Deleted = Value
}

case class MapState(mapMeta: RpgMap, 
                    dirty: Dirtiness.Value,
                    mapDataOpt: Option[RpgMapData]) 

class StateMaster(projectArgument: Project)
{
  var proj = projectArgument
  
  var maps: Map[Int, MapState] = {
    val states = proj.getMaps.map(rpgMap => 
      rpgMap.id->MapState(rpgMap, Dirtiness.Clean, None))
    
    Map(states : _*)
  }
  
  def getMapMetas = maps.values.map(_.mapMeta).toSeq
  
  // Must be sure that mapId exists and map data loaded to call
  def getMapMeta(mapId: Int) =
    maps.get(mapId).get.mapMeta
  
  def setMapMeta(mapId: Int, mapMeta: RpgMap) =
    maps = maps.updated(mapId,
      maps.get(mapId).get.copy(mapMeta = mapMeta, dirty = Dirtiness.Dirty)) 
  
  def getMapData(mapId: Int) = {
    assert(maps.contains(mapId), "map id %d doesn't exist".format(mapId))
    
    val mapState = maps.get(mapId).get
    maps.get(mapId).get.mapDataOpt getOrElse {
      val mapData = mapState.mapMeta.readMapData(proj) getOrElse {
        Dialog.showMessage(null, "Map data file missing. Recreating.", 
                           "Error", Dialog.Message.Error)
        RpgMap.emptyMapData(mapState.mapMeta.xSize, mapState.mapMeta.ySize)
      }
        
      maps = maps.updated(mapId, 
        mapState.copy(mapDataOpt = Some(mapData)))
      
      mapData
    }
  }
  
  def setMapData(mapId: Int, mapData: RpgMapData) = {
    maps = maps.updated(mapId,
      maps.get(mapId).get.copy(
        mapDataOpt = Some(mapData), dirty = Dirtiness.Dirty))
  }
}

