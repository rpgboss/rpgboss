package rpgboss.save

import rpgboss.model.Project
import rpgboss.lib.JsonUtils
import java.io.File
import rpgboss.model.MapLoc

case class SavedEventState(mapName: String, eventId: Int, eventState: Int)

case class SaveFile(
    intMap: Map[String, Int] = Map(),
    intArrayMap: Map[String, Array[Int]] = Map(),
    stringArrayMap: Map[String, Array[String]] = Map(),
    mapLocMap: Map[String, MapLoc] = Map(),
    eventStates: Array[SavedEventState] = Array())

object SaveFile {
  def file(project: Project) = new File(project.dir, "saves.json")

  def read(project: Project) = {
    JsonUtils.readModelFromJson[SaveFile](file(project)).getOrElse(SaveFile())
  }

  def write(saveFile: SaveFile, project: Project) =
    JsonUtils.writeModelToJson(file(project), saveFile)
}