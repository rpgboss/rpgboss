package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class Project(shortName: String, title: String, 
                   projectDir: File)
extends HasName
{
  def name = title
  
  def writeMetadata() : Boolean = {
    Project.filename(projectDir).prepareWrite({ fos =>
      ProjectSerial.newBuilder()
        .setTitle(title)
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
}

object Project {
  
  def startingProject(shortName: String, 
                      title: String, 
                      dir: File) =
    Project(shortName, title, dir)
  
  
  def filename(dir: File) = new File(dir, "project.rpgproject")
  
  def readFromDisk(projDir: File) = {
    val projFile = filename(projDir)
    
    if(projFile.canRead)
    {
      val serial = 
        ProjectSerial.parseFrom(new FileInputStream(projFile))
      
      Some(Project(projDir.getName, serial.getTitle, projDir))
    }
    else None
  }
}

