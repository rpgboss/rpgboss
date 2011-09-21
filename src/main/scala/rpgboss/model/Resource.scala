package rpgboss.model

import rpgboss.lib._

import java.io._
import FileHelper._
import scala.collection.JavaConversions._

object Resource {
  val resourceTypes = List(Autotile, Tileset)
  
  /*
  def listResources(p: Project) 
  : Map[String, List[String]] = 
  {
    val rcDir = new File(ObjPath.rcPath(owner, game))
    
    def walkResourceDir(rType: String) : (String, List[ObjName]) = {
      val rTypeDir = new File(rcDir, rType)
      
      val listOfResources = if(rTypeDir.makeWriteableDir()) 
        Nil 
      else // list of subdirectories (named after the resource name)
        rTypeDir.listFiles.toList.filter(f => f.isDirectory).map(_.toString)
      
      rType->listOfResources.map(rName => 
        ObjName(owner, game, Some(rType->rName))) 
    }
    
    if(rcDir.makeWriteableDir())
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
    else None*/
}

trait Resource[T] {
  def name: String
  def meta: MetaResource[T]
  def proj: Project
  
  def rcTypeDir = new File(proj.rcDir, meta.rcType)
  
  def writeMetadataToFos(fos: FileOutputStream)
  
  def writeToDisk() =
    writeMetadataToFos(new FileOutputStream(meta.metadataFile(proj, name)))
}

trait MetaResource[T] {
  def rcType: String
  def displayName: String
  def displayNamePlural: String
  
  def metadataFile(proj: Project, name: String) =
    new File(new File(proj.rcDir, rcType), "%s.%s".format(name, rcType))
  
  def defaultInstance(proj: Project, name: String) : T
  
  def fromMetadata(proj: Project, name: String, fis: FileInputStream) : T
    
  def readFromDisk(proj: Project, name: String) : T = {
    val mf = metadataFile(proj, name)
    
    if(mf.canRead)
      fromMetadata(proj, name, new FileInputStream(mf))
    else 
      defaultInstance(proj, name)
  }
  
  //def readFromDisk(name: String) : Option[T]
}


