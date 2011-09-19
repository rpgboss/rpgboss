package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import scala.collection.JavaConversions._
import java.io._

case class Project(shortName: String, title: String, 
                   projectDir: File,
                   autotiles: Vector[String],
                   recentMapId: Int)
extends HasName
{
  def name = title
  
  def writeMetadata() : Boolean = {
    Project.filename(projectDir).prepareWrite({ fos =>
      ProjectSerial.newBuilder()
        .setTitle(title)
        .addAllAutotiles(autotiles)
        .setRecentMapId(recentMapId)
      .build()
      .writeTo(fos)
      
      true
    })
  }
  
  def getMaps : Array[RpgMap] = {
    RpgMap.mapsDir(this)
      .listFiles.filter(_.getName.endsWith(RpgMap.metadataExt))
      .map(RpgMap.readMetadata).flatten
  }
  
  def rcDir = {
    new File(projectDir, "rc")
  }
}

object Project {
  
  def startingProject(shortName: String, 
                      title: String, 
                      dir: File) =
    Project(shortName, title, dir, defaultAutotiles, 1)
  
  
  def filename(dir: File) = new File(dir, "project.rpgproject")
  
  def readFromDisk(projDir: File) = {
    val projFile = filename(projDir)
    
    if(projFile.canRead)
    {
      val serial = 
        ProjectSerial.parseFrom(new FileInputStream(projFile))
      
      Some(Project(projDir.getName, serial.getTitle, projDir, 
                   Vector.empty ++ serial.getAutotilesList,
                   serial.getRecentMapId))
    }
    else None
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
    "Refmap-A3-0-0-B",
    "Refmap-A3-0-1-A",
    "Refmap-A3-0-1-B",
    "Refmap-A3-0-2-A",
    "Refmap-A3-0-2-B",
    "Refmap-A3-0-3-A",
    "Refmap-A3-0-3-B",
    "Refmap-A3-0-4-A",
    "Refmap-A3-0-4-B",
    "Refmap-A3-0-5-A",
    "Refmap-A3-0-5-B",
    "Refmap-A3-0-6-A",
    "Refmap-A3-0-6-B",
    "Refmap-A3-0-7-A",
    "Refmap-A3-0-7-B",
    "Refmap-A3-1-0-A",
    "Refmap-A3-1-0-B",
    "Refmap-A3-1-1-A",
    "Refmap-A3-1-1-B",
    "Refmap-A3-1-2-A",
    "Refmap-A3-1-2-B",
    "Refmap-A3-1-3-A",
    "Refmap-A3-1-3-B",
    "Refmap-A3-1-4-A",
    "Refmap-A3-1-4-B",
    "Refmap-A3-1-5-A",
    "Refmap-A3-1-5-B",
    "Refmap-A3-1-6-A",
    "Refmap-A3-1-6-B",
    "Refmap-A3-1-7-A",
    "Refmap-A3-1-7-B",
    "Refmap-A4-0-0-A",
    "Refmap-A4-0-0-B",
    "Refmap-A4-0-1-A",
    "Refmap-A4-0-1-B",
    "Refmap-A4-0-2-A",
    "Refmap-A4-0-2-B",
    "Refmap-A4-0-3-A",
    "Refmap-A4-0-3-B",
    "Refmap-A4-0-4-A",
    "Refmap-A4-0-4-B",
    "Refmap-A4-0-5-A",
    "Refmap-A4-0-5-B",
    "Refmap-A4-0-6-A",
    "Refmap-A4-0-6-B",
    "Refmap-A4-0-7-A",
    "Refmap-A4-0-7-B",
    "Refmap-A4-1-0-A",
    "Refmap-A4-1-0-B",
    "Refmap-A4-1-1-A",
    "Refmap-A4-1-1-B",
    "Refmap-A4-1-2-A",
    "Refmap-A4-1-2-B",
    "Refmap-A4-1-3-A",
    "Refmap-A4-1-3-B",
    "Refmap-A4-1-4-A",
    "Refmap-A4-1-4-B",
    "Refmap-A4-1-5-A",
    "Refmap-A4-1-5-B",
    "Refmap-A4-1-6-A",
    "Refmap-A4-1-6-B",
    "Refmap-A4-1-7-A",
    "Refmap-A4-1-7-B",
    "Refmap-A4-2-0-A",
    "Refmap-A4-2-0-B",
    "Refmap-A4-2-1-A",
    "Refmap-A4-2-1-B",
    "Refmap-A4-2-2-A",
    "Refmap-A4-2-2-B",
    "Refmap-A4-2-3-A",
    "Refmap-A4-2-3-B",
    "Refmap-A4-2-4-A",
    "Refmap-A4-2-4-B",
    "Refmap-A4-2-5-A",
    "Refmap-A4-2-5-B",
    "Refmap-A4-2-6-A",
    "Refmap-A4-2-6-B",
    "Refmap-A4-2-7-A",
    "Refmap-A4-2-7-B"
  )
}

