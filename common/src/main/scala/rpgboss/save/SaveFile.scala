package rpgboss.save

import rpgboss.model.Project
import rpgboss.lib.JsonUtils
import java.io.File
import rpgboss.model.MapLoc
import rpgboss.lib.Utils

case class SavedEventState(mapName: String, eventId: Int, eventState: Int)

case class SaveFile(
    intMap: Map[String, Int] = Map(),
    intArrayMap: Map[String, Array[Int]] = Map(),
    stringArrayMap: Map[String, Array[String]] = Map(),
    mapLocMap: Map[String, MapLoc] = Map(),
    eventStates: Array[SavedEventState] = Array())

object SaveFile {
  def file(project: Project, slot: Int) =
    new File(project.dir, Utils.generateFilename("save", slot, "json"))

  def read(project: Project, slot: Int) = {
    JsonUtils.readModelFromJson[SaveFile](
        file(project, slot: Int)).getOrElse(SaveFile())
  }

  def write(saveFile: SaveFile, project: Project, slot: Int) =
    JsonUtils.writeModelToJson(file(project, slot), saveFile)
}