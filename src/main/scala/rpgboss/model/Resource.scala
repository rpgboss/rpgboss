package rpgboss.model

import rpgboss.lib._

import java.io.File
import FileHelper._
import scala.collection.JavaConversions._

object Resource {
  val resourceTypes = Map("tileset"->Tileset)
  
  def listResources(owner: String, game: Option[String]) 
  : Map[String, List[ObjName]] = 
  {
    val rcDir = new File(ObjPath.rcPath(owner, game))
    
    def walkResourceDir(rType: String) : (String, List[ObjName]) = {
      val rTypeDir = new File(rcDir, rType)
      
      val listOfResources = if(rTypeDir.forceMkdirs()) 
        Nil 
      else // list of subdirectories (named after the resource name)
        rTypeDir.listFiles.toList.filter(f => f.isDirectory).map(_.toString)
      
      rType->listOfResources.map(rName => 
        ObjName(owner, game, Some(rType->rName))) 
    }
    
    if(rcDir.forceMkdirs())
      resourceTypes.mapValues( v => Nil )
    else {
      resourceTypes.map( kv => walkResourceDir(kv._1) )
    }
  }
  
  def readFromDisk(name: ObjName) : Option[Resource] = 
    if(name.exists) resourceTypes.get(name.rType) match {
      case Some(metaResource) => metaResource.readFromDisk(name)
      case None => None
    }
    else None
}

trait Resource {
  def name: ObjName
}

trait MetaResource[T <: Resource] {
  def resourceType: String
  def displayName: String
  def displayNamePlural: String
  
  def readFromDisk(name: ObjName) : Option[T]
}
