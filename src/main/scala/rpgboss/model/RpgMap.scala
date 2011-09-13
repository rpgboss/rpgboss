package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class RpgMap(id: Int,
                  parent: Int,
                  name: String,
                  xSize: Int,
                  ySize: Int,
                  tilesets: Array[String])
extends HasName
{
  
  def writeToDisk() = {
    true
  }
}

object RpgMap {
  def mapsDir(project: Project) = new File(project.metadata.projectDir, "maps")
  
  def metadataFile(project: Project, id: Int) = 
    new File(mapsDir(project), "Map%d.rpgmapmeta".format(id))
  def dataFile(project: Project, id: Int) = 
    new File(mapsDir(project), "Map%d.rpgmapdata".format(id))
    
  def firstMap = 
    RpgMap(1, -1, "Starting Map", 20, 15, Array("town"))
}
