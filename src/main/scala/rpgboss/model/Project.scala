package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class Project(shortName: String, gameTitle: String, projectsRoot: String)
{
  def writeToDisk() : Boolean = {
    val projectDir = new File(projectsRoot, shortName)
    val dataFile = new File(projectDir, Project.fileName)
    
    if(projectDir.makeWriteableDir() && dataFile.canWriteToFile()) {
      ProjectSerial.newBuilder()
        .setGameTitle(gameTitle)
      .build()
      .writeTo(new FileOutputStream(dataFile))
      
      true
    } else false
  }
}

object Project {
  val fileName = "project.rpgproject"
  
  def startingProject(shortName: String, 
                      gameTitle: String, 
                      projectsRoot: String) = 
    Project(shortName, gameTitle, projectsRoot)
}

