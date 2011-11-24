package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._

import java.io._

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

trait Resource[T, MT <: AnyRef] {
  def name: String
  def metadata: MT
  def meta: MetaResource[T, MT]
  def proj: Project
  
  def rcTypeDir = new File(proj.rcDir, meta.rcType)
  
  def writeMetadata() : Boolean =
    meta.metadataFile(proj, name).getWriter().map { writer =>
      implicit val formats = net.liftweb.json.DefaultFormats
      Serialization.write(metadata, writer) != null
    } getOrElse false
}

trait MetaResource[T, MT] {
  def rcType: String
  
  def metadataFile(proj: Project, name: String) =
    new File(new File(proj.rcDir, rcType), "%s.%s.json".format(name, rcType))
  
  def defaultInstance(proj: Project, name: String) : T
  
  def apply(proj: Project, name: String, metadata: MT) : T
  
  // Returns default instance in case of failure to retrieve
  
  def readFromDisk(proj: Project, name: String) : T = {
    implicit val formats = net.liftweb.json.DefaultFormats
    metadataFile(proj, name).getReader().map { reader => 
      apply(proj, name, Serialization.read(reader))
    } getOrElse defaultInstance(proj, name)
  }
}


