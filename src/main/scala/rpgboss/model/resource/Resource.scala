package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.lib.FileHelper._
import net.liftweb.json._
import scala.collection.JavaConversions._
import java.io._
import rpgboss.model.Project
import com.weiglewilczek.slf4s.Logging

object Resource {
  val formats = Serialization.formats(ShortTypeHints(
    List(classOf[Autotile], classOf[Tileset])))
}

trait Resource[T, MT <: AnyRef] extends Logging {
  def name: String
  def metadata: MT
  def meta: MetaResource[T, MT]
  def proj: Project
  
  def rcTypeDir = new File(proj.rcDir, meta.rcType)
  val dataFileVar = new File(rcTypeDir, name)
  if(!(dataFileVar.isFile && dataFileVar.canRead))
    logger.error("Can't read: %s".format(dataFileVar.getCanonicalPath()))
    
  def dataFile = dataFileVar
  val absPath = dataFile.getAbsolutePath()
  
  def writeMetadata() : Boolean =
    meta.metadataFile(proj, name).useWriter { writer =>
      implicit val formats = Resource.formats
      Serialization.writePretty(metadata, writer) != null
    } getOrElse false
}

trait MetaResource[T, MT] {
  def rcType: String
  def keyExts : Array[String] // extension to search for when listing resources
  
  def rcDir(proj: Project) = new File(proj.rcDir, rcType)
  
  // not guaranteed to be in any particular order
  def list(proj: Project) = {
    val listsByType = keyExts.map { keyExt =>    
      rcDir(proj).listFiles.map(_.getName)
        .filter(_.endsWith(keyExt))
    }
    
    Array.concat(listsByType : _*)
  }
  
  def metadataFile(proj: Project, name: String) =
    new File(rcDir(proj), "%s.%s.json".format(name, rcType))
  
  def defaultInstance(proj: Project, name: String) : T
  
  def apply(proj: Project, name: String, metadata: MT) : T
  
  // Returns default instance in case of failure to retrieve
  
  def readFromDisk(proj: Project, name: String) : T = {
    implicit val formats = Resource.formats
    metadataFile(proj, name).getReader().map { reader => 
      apply(proj, name, Serialization.read(reader))
    } getOrElse defaultInstance(proj, name)
  }
}

case class ResourceException(msg: String) extends Exception(msg)

