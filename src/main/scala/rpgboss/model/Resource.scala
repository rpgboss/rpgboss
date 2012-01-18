package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json._

import scala.collection.JavaConversions._

import java.io._

object Resource {
  val formats = Serialization.formats(ShortTypeHints(
    List(classOf[Autotile], classOf[Tileset], classOf[RpgMapMetadata])))
}

trait Resource[T, MT <: AnyRef] {
  def name: String
  def metadata: MT
  def meta: MetaResource[T, MT]
  def proj: Project
  
  def rcTypeDir = new File(proj.rcDir, meta.rcType)
  
  def writeMetadata() : Boolean =
    meta.metadataFile(proj, name).getWriter().map { writer =>
      implicit val formats = Resource.formats
      Serialization.write(metadata, writer) != null
    } getOrElse false
}

trait MetaResource[T, MT] {
  def rcType: String
  val metadataExt = "%s.json".format(rcType) 
  def keyExt : String // extension to search for when listing resources
  
  def rcDir(proj: Project) = new File(proj.rcDir, rcType)
  def list(proj: Project) = 
    rcDir(proj).listFiles.map(_.getName)
      .filter(_.endsWith(keyExt))
      .map(_.dropRight(keyExt.length+1)) // +1 to drop the dot before the name
  
  def metadataFile(proj: Project, name: String) =
    new File(rcDir(proj), "%s.%s".format(name, metadataExt))
  
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

