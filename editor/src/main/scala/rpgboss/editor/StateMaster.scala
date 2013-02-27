package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._
import rpgboss.editor.cache._

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
      RpgMap.metadataFile(p, map.name).delete()
      RpgMapData.dataFile(p, map.name).delete()
    }
  }
}

/**
 * This class manages the dirtiness and saving of all the open maps.
 */
class StateMaster(mainPanel: MainPanel, private var proj: Project)
{
  import Dirtiness._
  
  val assetCache = new AssetCache(proj)
  
  private var projDirty = Dirtiness.Clean
  
  private val mapStates = collection.mutable.Map[String, MapState]()
  
  def loadProjectData() = {
    RpgMap.list(proj).map(RpgMap.readFromDisk(proj, _)).foreach(map => {
      addMap(map, None, Dirtiness.Clean)
    })
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
    
    mainPanel.updateDirty(this)
  }
  
  def stateDirty =
    projDirty != Clean || mapStates.values.exists(_.dirty != Clean)
  
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
    mainPanel.updateDirty(this)
  }
  
  def addMap(
      map: RpgMap, 
      mapDataOpt: Option[RpgMapData], 
      dirty: Dirtiness.Value) = {
    mapStates.put(map.name, MapState(map, dirty, mapDataOpt))
  }
  
  def getMapStates = mapStates
  
  def getMapMetas = mapStates.values.map(_.map).toSeq
  
  // Must be sure that mapId exists and map data loaded to call
  def getMap(mapName: String) =
    mapStates.get(mapName).get.map
  
  def setMap(mapName: String, map: RpgMap, markDirty: Boolean = true) = {
    val curState = mapStates.get(mapName).get
    val newDirty = if(markDirty) Dirtiness.Dirty else curState.dirty 
    mapStates.update(mapName, curState.copy(map = map, dirty = newDirty))
    mainPanel.updateDirty(this)
  }
  
  def getMapData(mapName: String) = {
    assert(mapStates.contains(mapName), "map %d doesn't exist".format(mapName))
    val mapState = mapStates.get(mapName).get
    mapState.mapDataOpt getOrElse {
      val mapData = mapState.map.readMapData() getOrElse {
        Dialog.showMessage(null, "Map data file missing. Recreating.", 
                           "Error", Dialog.Message.Error)
        RpgMap.emptyMapData(mapState.map.metadata.xSize, 
                            mapState.map.metadata.ySize)
      }
        
      mapStates.update(mapName, 
        mapState.copy(mapDataOpt = Some(mapData)))
      
      mapData
    }
  }
  
  def setMapData(mapName: String, mapData: RpgMapData) = {
    mapStates.update(mapName,
      mapStates.get(mapName).get.copy(
        mapDataOpt = Some(mapData), dirty = Dirtiness.Dirty))
    mainPanel.updateDirty(this)
  }
}

