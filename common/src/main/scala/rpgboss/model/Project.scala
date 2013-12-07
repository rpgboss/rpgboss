package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import org.json4s.native.Serialization

import scala.collection.JavaConversions._
import java.io._

case class Project(dir: File, data: ProjectData) {
  def writeMetadata(): Boolean = data.write(dir)

  def rcDir = dir
}

object Project {

  def startingProject(title: String,
                      dir: File) =
    Project(dir, ProjectData(
      uuid = java.util.UUID.randomUUID().toString(),
      title = title))

  def readFromDisk(projDir: File): Option[Project] =
    ProjectData.read(projDir).map(Project(projDir, _))
}

