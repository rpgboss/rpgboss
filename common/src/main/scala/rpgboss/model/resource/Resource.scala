package rpgboss.model.resource

import rpgboss.lib._
import scala.collection.JavaConversions._
import java.io._
import rpgboss.model.Project
import com.typesafe.scalalogging.slf4j.Logging

trait Resource[T, MT <: AnyRef] extends Logging {
  def name: String
  def metadata: MT
  def meta: MetaResource[T, MT]
  def proj: Project

  def rcTypeDir = new File(proj.rcDir, meta.rcType)
  val dataFileVar = new File(rcTypeDir, name)

  def dataFile = dataFileVar

  def writeMetadata(): Boolean =
    JsonUtils.writeModelToJson(meta.metadataFile(proj, name), metadata)
}

trait MetaResource[T, MT] {
  def rcType: String
  def keyExts: Array[String] // extension to search for when listing resources

  def rcDir(proj: Project) = new File(proj.rcDir, rcType)

  /**
   * Lists files matching the extension filter in the resource directory, as
   * well as direct child subdirectories.
   */
  def list(proj: Project): Array[String] = {
    val items = collection.mutable.Buffer[String]()

    def extensionFilter(file: File): Boolean = {
      for(ext <- keyExts) {
        if(file.getName.endsWith(ext))
          return true
      }
      return false
    }

    val resourceDir = rcDir(proj)
    if (!resourceDir.exists())
      resourceDir.mkdir()
    if (resourceDir.isFile())
      return Array()

    for(rootFile <- resourceDir.listFiles()) {
      if(rootFile.isFile() && extensionFilter(rootFile)) {
        items.append(rootFile.getName())
      } else if(rootFile.isDirectory()) {
        for(subFile <- rootFile.listFiles()) {
          if(subFile.isFile() && extensionFilter(subFile)) {
            items.append(rootFile.getName() + "/" + subFile.getName())
          }
        }
      }
    }

    items.sortWith(_ < _).toArray
  }

  def metadataFile(proj: Project, name: String) = {
    val resourceFile = new File(rcDir(proj), name)
    val resourceFilename = resourceFile.getName()
    val resourceDir = resourceFile.getParentFile()

    new File(resourceDir, "%s.metadata.json".format(resourceFilename))
  }

  // Create a new instance with the default metadata
  def defaultInstance(proj: Project, name: String): T

  def apply(proj: Project, name: String, metadata: MT): T

  // Returns default instance in case of failure to retrieve
  def readFromDisk(proj: Project, name: String)(implicit m: Manifest[MT]): T = {
    val metadataOpt =
      JsonUtils.readModelFromJson[MT](metadataFile(proj, name))(m)
    metadataOpt.map(apply(proj, name, _)).getOrElse(defaultInstance(proj, name))
  }
}

case class ResourceException(msg: String) extends Exception(msg)

object Resource {
  val resourceTypes = List(
      AnimationImage, Autotile, Battler, Iconset, Msgfont, Music, Picture,
      RpgMap, Script, Sound, Spriteset, Tileset, Windowskin)
}