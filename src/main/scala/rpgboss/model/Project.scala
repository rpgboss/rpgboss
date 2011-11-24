package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._
import java.io._

case class ProjectData(title: String, 
                       autotiles: Vector[String],
                       recentMapId: String) 

case class Project(dir: File, data: ProjectData)
{
  def writeMetadata() : Boolean = 
    Project.filename(dir).getWriter().map { writer =>
      implicit val formats = net.liftweb.json.DefaultFormats
      Serialization.write(data, writer) != null
    } getOrElse false
  
  def getMaps : Array[RpgMap] = {
    Resource.listResources(mapsDir, RpgMap.metadataExt)
      .map(RpgMap.readFromDisk(this, _))
  }
  
  def rcDir   = new File(dir, "rc")
  def mapsDir = new File(dir, "maps")
}

object Project {
  
  def startingProject(title: String, 
                      dir: File) =
    Project(dir, ProjectData(title, defaultAutotiles, ""))
  
  
  def filename(dir: File) = new File(dir, "rpgproject.json")
  
  def readFromDisk(projDir: File) : Option[Project] =
    filename(projDir).readAsString.map { str =>
      implicit val formats = net.liftweb.json.DefaultFormats
      /*println("Test reader:")
      rdr.mark(500000)
      while(rdr.ready()) {
        println(rdr.readLine())
      }
      rdr.reset()
      println(projDir)
      println("Start serialization read")
      println(rdr)
      */
      println(str)
      println("String")
      val pd = Serialization.read[ProjectData](str)
      println("Finish serialization read:")
      println(pd)
      Project(projDir, pd)
    }
  
  def defaultAutotiles = Vector(
    "Refmap-A1-0-0-A",
    "Refmap-A1-0-0-B",
    "Refmap-A1-0-1-A",
    "Refmap-A1-0-1-B",
    "Refmap-A1-1-0-A",
    "Refmap-A1-1-0-B",
    "Refmap-A1-1-1-A",
    "Refmap-A1-1-1-B",
    "Refmap-A1-2-0-A",
    "Refmap-A1-2-0-B",
    "Refmap-A1-2-1-A",
    "Refmap-A1-2-1-B",
    "Refmap-A1-3-0-A",
    "Refmap-A1-3-0-B",
    "Refmap-A1-3-1-A",
    "Refmap-A1-3-1-B",
    "Refmap-A2-0-0",
    "Refmap-A2-0-1",
    "Refmap-A2-0-2",
    "Refmap-A2-0-3",
    "Refmap-A2-0-4",
    "Refmap-A2-0-5",
    "Refmap-A2-0-6",
    "Refmap-A2-0-7",
    "Refmap-A2-1-0",
    "Refmap-A2-1-1",
    "Refmap-A2-1-2",
    "Refmap-A2-1-3",
    "Refmap-A2-1-4",
    "Refmap-A2-1-5",
    "Refmap-A2-1-6",
    "Refmap-A2-1-7",
    "Refmap-A2-2-0",
    "Refmap-A2-2-1",
    "Refmap-A2-2-2",
    "Refmap-A2-2-3",
    "Refmap-A2-2-4",
    "Refmap-A2-2-5",
    "Refmap-A2-2-6",
    "Refmap-A2-2-7",
    "Refmap-A2-3-0",
    "Refmap-A2-3-1",
    "Refmap-A2-3-2",
    "Refmap-A2-3-3",
    "Refmap-A2-3-4",
    "Refmap-A2-3-5",
    "Refmap-A2-3-6",
    "Refmap-A2-3-7",
    "Refmap-A3-0-0-A",
    "Refmap-A3-0-1-A",
    "Refmap-A3-0-2-A",
    "Refmap-A3-0-3-A",
    "Refmap-A3-0-4-A",
    "Refmap-A3-0-5-A",
    "Refmap-A3-0-6-A",
    "Refmap-A3-0-7-A",
    "Refmap-A3-1-0-A",
    "Refmap-A3-1-1-A",
    "Refmap-A3-1-2-A",
    "Refmap-A3-1-3-A",
    "Refmap-A3-1-4-A",
    "Refmap-A3-1-5-A",
    "Refmap-A3-1-6-A",
    "Refmap-A3-1-7-A",
    "Refmap-A3-2-0-A",
    "Refmap-A3-2-1-A",
    "Refmap-A3-2-2-A",
    "Refmap-A3-2-3-A",
    "Refmap-A3-2-4-A",
    "Refmap-A3-2-5-A",
    "Refmap-A3-2-6-A",
    "Refmap-A3-2-7-A",
    "Refmap-A3-3-0-A",
    "Refmap-A3-3-1-A",
    "Refmap-A3-3-2-A",
    "Refmap-A3-3-3-A",
    "Refmap-A3-3-4-A",
    "Refmap-A3-3-5-A",
    "Refmap-A3-3-6-A",
    "Refmap-A3-3-7-A",
    "Refmap-A4-0-0",
    "Refmap-A4-0-1",
    "Refmap-A4-0-2",
    "Refmap-A4-0-3",
    "Refmap-A4-0-4",
    "Refmap-A4-0-5",
    "Refmap-A4-0-6",
    "Refmap-A4-0-7",
    "Refmap-A4-1-0",
    "Refmap-A4-1-1",
    "Refmap-A4-1-2",
    "Refmap-A4-1-3",
    "Refmap-A4-1-4",
    "Refmap-A4-1-5",
    "Refmap-A4-1-6",
    "Refmap-A4-1-7",
    "Refmap-A4-2-0",
    "Refmap-A4-2-1",
    "Refmap-A4-2-2",
    "Refmap-A4-2-3",
    "Refmap-A4-2-4",
    "Refmap-A4-2-5",
    "Refmap-A4-2-6",
    "Refmap-A4-2-7",
    "Refmap-A4-3-0",
    "Refmap-A4-3-1",
    "Refmap-A4-3-2",
    "Refmap-A4-3-3",
    "Refmap-A4-3-4",
    "Refmap-A4-3-5",
    "Refmap-A4-3-6",
    "Refmap-A4-3-7",
    "Refmap-A4-4-0",
    "Refmap-A4-4-1",
    "Refmap-A4-4-2",
    "Refmap-A4-4-3",
    "Refmap-A4-4-4",
    "Refmap-A4-4-5",
    "Refmap-A4-4-6",
    "Refmap-A4-4-7",
    "Refmap-A4-5-0",
    "Refmap-A4-5-1",
    "Refmap-A4-5-2",
    "Refmap-A4-5-3",
    "Refmap-A4-5-4",
    "Refmap-A4-5-5",
    "Refmap-A4-5-6",
    "Refmap-A4-5-7"
  )
}

