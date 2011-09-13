package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class ProjectMetadata(shortName: String, title: String, 
                           projectDir: File)
extends HasName
{
  def name = title
  
  def writeToDisk() : Boolean = {
    val dataFile = new File(projectDir, ProjectMetadata.fileName)
    
    if(projectDir.makeWriteableDir() && dataFile.canWriteToFile()) {
      ProjectSerial.newBuilder()
        .setTitle(title)
      .build()
      .writeTo(new FileOutputStream(dataFile))
      
      true
    } else false
  }
}

case class Project(metadata: ProjectMetadata, maps: Vector[RpgMap])
{ 
  def saveAll() : Boolean = 
    metadata.writeToDisk() && maps.map(_.writeToDisk()).reduceLeft(_&&_)
}

object Project {
  
  def startingProject(shortName: String, 
                      title: String, 
                      dir: File) = 
  {
    val m = ProjectMetadata(shortName, title, dir)                    
    Project(m, Vector(RpgMap.firstMap))
  }
  
}

object ProjectMetadata {
  val fileName = "project.rpgproject"
  
  def readFromDisk(dir: File) = {
    val projFile = new File(dir, fileName)
    
    if(projFile.canRead)
    {
      val serial = 
        ProjectSerial.parseFrom(new FileInputStream(projFile))
      
      Some(ProjectMetadata(dir.getName, serial.getTitle, dir))
    }
    else None
  }
}

