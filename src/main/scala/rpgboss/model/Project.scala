package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._
import java.io._

case class Project(dir: File, data: ProjectData)
{
  def writeMetadata() : Boolean = 
    Project.filename(dir).useWriter { writer =>
      implicit val formats = net.liftweb.json.DefaultFormats
      Serialization.writePretty(data, writer) != null
    } getOrElse false
  
  def rcDir   = new File(dir, "rc")
  def mapsDir = new File(dir, "maps")
}

object Project {
  
  def startingProject(title: String, 
                      dir: File) =
    Project(dir, ProjectData.defaultInstance(title))
  
  def filename(dir: File) = new File(dir, "rpgproject.json")
  
  def readFromDisk(projDir: File) : Option[Project] =
    filename(projDir).readAsString.map { str =>
      implicit val formats = net.liftweb.json.DefaultFormats
      val pd = Serialization.read[ProjectData](str)
      Project(projDir, pd)
    }
}

