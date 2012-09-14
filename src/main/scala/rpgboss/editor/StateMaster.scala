package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._

import rpgboss.model._
import rpgboss.model.resource._

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
      RpgMap.metadataFile(p, map.id).delete()
      RpgMapData.dataFile(p, map.name).delete()
    }
  }
}

class StateMaster(private var proj: Project)
{
  import Dirtiness._
  
  private var projDirty = Dirtiness.Clean
  
  private var autotiles: Array[Autotile] = null
  private var mapStates: Map[Int, MapState] = null
  
  def loadProjectData() = {
    autotiles =
      proj.data.autotiles.toArray.map(Autotile.readFromDisk(proj, _))
    
    val states = RpgMap.list(proj).map(RpgMap.readFromDisk(proj, _)).map(
      rpgMap => rpgMap.id->MapState(rpgMap, Dirtiness.Clean, None))
    
    mapStates = Map(states : _*)
  }
  
  loadProjectData()
  
  // maps map is cleared of excess data upon saving
  def save() = {
    // save project (database, etc.)
    if(projDirty == Dirty) {
      if(proj.writeMetadata()) {
        projDirty = Clean
      }
    }
    
    mapStates.values.map(_.save(proj)) // save all the maps
    
    loadProjectData() // refresh stale shit
  }
  
  def stateDirty =
    projDirty == Clean && mapStates.values.exists(_.dirty != Clean)
  
  def askSaveUnchanged(diagParent: Component) = {
    if(stateDirty) {
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
  
  def getProj = proj
  def setProj(newProj: Project) = {
    proj = newProj
    projDirty = Dirty
  }
  
  def getMapStates = mapStates
  
  def getAutotiles = autotiles
  
  def getMapMetas = mapStates.values.map(_.map).toSeq
  
  // Must be sure that mapId exists and map data loaded to call
  def getMap(mapId: Int) =
    mapStates.get(mapId).get.map
  
  def setMap(mapId: Int, map: RpgMap) =
    mapStates = mapStates.updated(mapId,
      mapStates.get(mapId).get.copy(map = map, dirty = Dirtiness.Dirty)) 
  
  def getMapData(mapId: Int) = {
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
  
  def setMapData(mapId: Int, mapData: RpgMapData) = {
    mapStates = mapStates.updated(mapId,
      mapStates.get(mapId).get.copy(
        mapDataOpt = Some(mapData), dirty = Dirtiness.Dirty))
  }
}

