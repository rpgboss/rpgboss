package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._

import rpgboss.model._
import rpgboss.message._

import scala.swing._

object Dirtiness extends Enumeration {
  val Clean, Dirty, Deleted = Value
}

case class MapState(mapMeta: RpgMap, 
                    dirty: Dirtiness.Value,
                    mapDataOpt: Option[RpgMapData]) 
{
  import Dirtiness._
  def save(p: Project) = {
    if(dirty == Dirty) {
      // Save if it's dirty
      mapMeta.saveMetadata(p)
      mapDataOpt.map(data => mapMeta.saveMapData(p, data))
    } else if(dirty == Deleted) {
      // Effect deletion
      RpgMap.metadataFile(p, mapMeta.id).delete()
      RpgMap.dataFile(p, mapMeta.id).delete()
    }
  }
}

class StateMaster(val proj: Project) extends SelectsMap
{
  import Dirtiness._
  
  def loadUpMaps() = {
    val states = proj.getMaps.map(rpgMap => 
      rpgMap.id->MapState(rpgMap, Dirtiness.Clean, None))
    
    Map(states : _*)
  }
  
  var maps: Map[Int, MapState] = loadUpMaps() 
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    // Leaving it be for now, as don't understand how to optimize yet
    /* Use this opportunity to clear out unneeded stuff in memory
    maps = maps.map {
      case (id, mapState) if mapState.dirty == Clean =>
         (id, mapState.copy(mapDataOpt = None))
    }*/
  }
  
  def save() = {
    maps.values.map(_.save(proj)) // save it all
    maps = loadUpMaps() // refresh stale shit
  }
  
  def askSaveUnchanged(diagParent: Component) = {
    if(maps.values.exists(_.dirty != Clean)) {
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
  
  var autotiles = proj.autotiles.map(Autotile.readFromDisk(proj, _))
  
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

