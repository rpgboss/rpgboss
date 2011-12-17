package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._

import rpgboss.model._

import scala.swing._

object Dirtiness extends Enumeration {
  val Clean, Dirty, Deleted = Value
}

case class MapState(map: RpgMap, 
                    dirty: Dirtiness.Value,
                    mapDataOpt: Option[RpgMapData]) 
{
  import Dirtiness._
  def save(p: Project) = {
    if(dirty == Dirty) {
      // Save if it's dirty
      map.writeMetadata()
      mapDataOpt.map(data => map.saveMapData(data))
    } else if(dirty == Deleted) {
      // Effect deletion
      RpgMap.metadataFile(p, map.name).delete()
      RpgMapData.dataFile(p, map.name).delete()
    }
  }
}

class StateMaster(val proj: Project)
{
  import Dirtiness._
  
  def loadUpMaps() = {
    val states = proj.getMaps.map(rpgMap => 
      rpgMap.name->MapState(rpgMap, Dirtiness.Clean, None))
    println(states.deep)
    
    Map(states : _*)
  }
  
  var mapStates: Map[String, MapState] = loadUpMaps()
  
  // maps map is cleared of excess data upon saving
  def save() = {
    mapStates.values.map(_.save(proj)) // save it all
    mapStates = loadUpMaps() // refresh stale shit
  }
  
  def askSaveUnchanged(diagParent: Component) = {
    if(mapStates.values.exists(_.dirty != Clean)) {
      Dialog.showConfirmation(diagParent,
                              "Save changes to project?",
                              "rpgboss",
                              Dialog.Options.YesNoCancel) match 
      {
        case Dialog.Result.Yes => 
          save()
          true
        case Dialog.Result.No => true
        case Dialog.Result.Cancel => false
        case _ => false
      }
    } else true
  }
  
  var autotiles = 
    proj.data.autotiles.toArray.map(Autotile.readFromDisk(proj, _))
  
  def getMapMetas = mapStates.values.map(_.map).toSeq
  
  // Must be sure that mapId exists and map data loaded to call
  def getMap(mapId: String) =
    mapStates.get(mapId).get.map
  
  def setMap(mapId: String, map: RpgMap) =
    mapStates = mapStates.updated(mapId,
      mapStates.get(mapId).get.copy(map = map, dirty = Dirtiness.Dirty)) 
  
  def getMapData(mapId: String) = {
    assert(mapStates.contains(mapId), "map id %d doesn't exist".format(mapId))
    val mapState = mapStates.get(mapId).get
    mapState.mapDataOpt getOrElse {
      val mapData = mapState.map.readMapData() getOrElse {
        Dialog.showMessage(null, "Map data file missing. Recreating.", 
                           "Error", Dialog.Message.Error)
        RpgMap.emptyMapData(mapState.map.metadata.xSize, 
                            mapState.map.metadata.ySize)
      }
        
      mapStates = mapStates.updated(mapId, 
        mapState.copy(mapDataOpt = Some(mapData)))
      
      mapData
    }
  }
  
  def setMapData(mapId: String, mapData: RpgMapData) = {
    mapStates = mapStates.updated(mapId,
      mapStates.get(mapId).get.copy(
        mapDataOpt = Some(mapData), dirty = Dirtiness.Dirty))
  }
}

