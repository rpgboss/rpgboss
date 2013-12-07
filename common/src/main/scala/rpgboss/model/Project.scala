package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import org.json4s.native.Serialization

import scala.collection.JavaConversions._
import java.io._

case class Project(dir: File, data: ProjectData) {
  def writeMetadata(): Boolean =
    JsonUtils.writeModelToJson(Project.filename(dir), data)

  def rcDir = dir
}

object Project {

  def startingProject(title: String,
                      dir: File) =
    Project(dir, ProjectData(
      uuid = java.util.UUID.randomUUID().toString(),
      title = title))

  def filename(dir: File) = new File(dir, "rpgproject.json")

  def readFromDisk(projDir: File): Option[Project] =
    JsonUtils.readModelFromJson(filename(projDir)).map(Project(projDir, _))
}

