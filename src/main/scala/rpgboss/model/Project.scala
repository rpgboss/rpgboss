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
    ProjectMetadata.filename(projectDir).prepareWrite({ fos =>
      ProjectSerial.newBuilder()
        .setTitle(title)
      .build()
      .writeTo(fos)
      
      true
    })
  }
}

case class Project(metadata: ProjectMetadata, maps: Vector[RpgMap])
{ 
  def saveAll() : Boolean = 
    metadata.writeToDisk() && maps.map(_.writeToDisk(this)).reduceLeft(_&&_)
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
  def filename(dir: File) = new File(dir, "project.rpgproject")
  
  def readFromDisk(dir: File) = {
    val projFile = filename(dir)
    
    if(projFile.canRead)
    {
      val serial = 
        ProjectSerial.parseFrom(new FileInputStream(projFile))
      
      Some(ProjectMetadata(dir.getName, serial.getTitle, dir))
    }
    else None
  }
}

