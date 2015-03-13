package rpgboss.model

import java.io.File
import rpgboss.lib.JsonUtils
import rpgboss.model.event.EventClass
import rpgboss.model.resource.ResourceConstants
import rpgboss.lib.DistinctCharacterSet

case class ProjectDataStartup(
  var startingLoc: MapLoc,
  var startingParty: Array[Int],

  var titlePic: String,
  var titleMusic: Option[SoundSpec],

  var gameOverPic: String,
  var gameOverMusic: Option[SoundSpec],

  var screenW: Int,
  var screenH: Int,

  var windowskin: String,

  var msgfont: String,
  var fontsize: Int,

  var soundCursor: Option[SoundSpec],
  var soundSelect: Option[SoundSpec],
  var soundCancel: Option[SoundSpec],
  var soundCannot: Option[SoundSpec])

object ProjectDataStartup {
  def apply(): ProjectDataStartup = ResourceConstants.getProjectDataStartup
}

case class ProjectDataEnums(
  var animations: Array[Animation] = Array(Animation()),
  var characters: Array[Character] = ResourceConstants.defaultCharacters,
  var classes: Array[CharClass] = Array(CharClass()),
  var elements: Array[String] = ProjectData.defaultElements,
  var enemies: Array[Enemy] = Array(Enemy()),
  var encounters: Array[Encounter] = Array(Encounter()),
  var eventClasses: Array[EventClass] = Array(EventClass()),
  var equipTypes: Array[String] = ProjectData.defaultEquipTypes,
  var translationTypes: Array[String] = ProjectData.defaultTranslation,
  var items: Array[Item] = Array(Item()),
  var skills: Array[Skill] = Array(Skill()),
  var statusEffects: Array[StatusEffect] = Array(StatusEffect())) {
  lazy val distinctChars = {
    val set = new DistinctCharacterSet
    set.addAll(characters)
    set.addAll(characters.map(_.description))
    set.addAll(classes)
    set.addAll(elements)
    set.addAll(enemies)
    set.addAll(encounters)
    set.addAll(equipTypes)
    set.addAll(items)
    set.addAll(items.map(_.desc))
    set.addAll(skills)
    set.addAll(statusEffects)
    set.addAll(translationTypes)

    for (eventClass <- eventClasses; state <- eventClass.states) {
      set ++= state.distinctChars
    }

    set
  }
}

case class ProjectData(
  var uuid: String = java.util.UUID.randomUUID().toString(),
  var title: String = "Untitled Project",
  var recentMapName: String = "",
  var lastCreatedMapId: Int = 1, // Start at 1)
  var startup: ProjectDataStartup = ProjectDataStartup(),
  var enums: ProjectDataEnums = ProjectDataEnums(),
  var messages: Map[String, String] = ProjectData.defaultMessages) {

  def writeEnums(dir: File) = {
    def writeModel[T <: AnyRef](name: String, model: T) =
      JsonUtils.writeModelToJson(new File(dir, "%s.json".format(name)), model)

    writeModel("animations", enums.animations)
    writeModel("characters", enums.characters)
    writeModel("classes", enums.classes)
    writeModel("elements", enums.elements)
    writeModel("enemies", enums.enemies)
    writeModel("encounters", enums.encounters)
    writeModel("eventClasses", enums.eventClasses)
    writeModel("equipTypes", enums.equipTypes)
    writeModel("items", enums.items)
    writeModel("skills", enums.skills)
    writeModel("statusEffects", enums.statusEffects)
    writeModel("translationTypes", enums.translationTypes)
  }

  def writeRootWithoutEnums(dir: File) = {
    val enumsStripped = copy(enums = null)
    def writeModel[T <: AnyRef](name: String, model: T) =
      JsonUtils.writeModelToJson(new File(dir, "%s.json".format(name)), model)

    writeModel("rpgproject", enumsStripped)
  }

  def write(dir: File) = {
    writeRootWithoutEnums(dir)
    writeEnums(dir)
  }
}

object ProjectData {
  def read(dir: File): Option[ProjectData] = {
    val modelOpt = JsonUtils.readModelFromJson[ProjectData](
      new File(dir, "rpgproject.json"))

    // Executes the setter only if it successfully reads it from file.
    def readModel[T <: AnyRef](
      name: String, setter: T => Unit)(implicit m: Manifest[T]) = {
      val opt =
        JsonUtils.readModelFromJson[T](new File(dir, "%s.json".format(name)))

      if (opt.isDefined)
        setter(opt.get)
    }

    modelOpt.foreach { model =>
      val enums = model.enums
      readModel[Array[Animation]]("animations", enums.animations = _)
      readModel[Array[Character]]("characters", enums.characters = _)
      readModel[Array[CharClass]]("classes", enums.classes = _)
      readModel[Array[String]]("elements", enums.elements = _)
      readModel[Array[Enemy]]("enemies", enums.enemies = _)
      readModel[Array[Encounter]]("encounters", enums.encounters = _)
      readModel[Array[EventClass]]("eventClasses", enums.eventClasses = _)
      readModel[Array[String]]("equipTypes", enums.equipTypes = _)
      readModel[Array[Item]]("items", enums.items = _)
      readModel[Array[Skill]]("skills", enums.skills = _)
      readModel[Array[StatusEffect]]("statusEffects", enums.statusEffects = _)
      readModel[Array[String]]("translationTypes", enums.translationTypes = _)
    }

    modelOpt
  }

  def defaultElements = Array(
    "Untyped",

    "Blunt",
    "Piercing",
    "Slashing",

    "Fire",
    "Cold",
    "Electric",
    "Earth",

    "Life",
    "Death",
    "Order",
    "Chaos")

  def defaultTranslation = Array(
    // start menu
    "Start Game",
    "Load Game",
    "Quit",
    // ingame menu
    "Item", 
    "Skills", 
    "Equip", 
    "Status", 
    "Save", 
    "Quit Game",
    // game over menu
    "Back to titlescreen",
    "Quit game",
    // save menu
    "Save",
    "Empty",
    // item menu
    "Use",
    "Organize",
    // equip menu
    "Max HP:",
    "Max MP:",
    "ATK:",
    "SPD:",
    "MAG:",
    "ARM:",
    "MRE:",
    "Weapon", 
    "Offhand", 
    "Armor", 
    "Accessory", 
    "Accessory",
    // status menu
    "LVL",
    "HP",
    "MP",
    // store menu
    "Gold:",
    "Owned:",
    "Buy",
    "Sell"
  )

  def defaultEquipTypes = Array(
    "Weapon",
    "Offhand",
    "Armor",
    "Head",
    "Accessory")

  def defaultMessages = Map(
    // start menu
    "New Game" -> "New Game",
    "Load Game" -> "Load Game",
    "Quit" -> "Quit",
    // ingame menu
    "Item" -> "Item",
    "Skills" -> "Skills",
    "Equip" -> "Equip",
    "Status" -> "Status",
    "Save" -> "Save",
    "Quit Game" -> "Quit Game",
    // game over menu
    "Back to titlescreen" -> "Back to titlescreen",
    "Quit game" -> "Quit game",
    // save menu
    "Save" -> "Save",
    "Empty" -> "Empty",
    // item menu
    "Use" -> "Use",
    "Organize" -> "Organize",
    // equip menu
    "Max HP:" -> "Max HP:",
    "Max MP:" -> "Max MP:",
    "ATK:" -> "ATK:",
    "SPD:" -> "SPD:",
    "MAG:" -> "MAG:",
    "ARM:" -> "ARM:",
    "MRE:" -> "MRE:",
    "Weapon" -> "Weapon",
    "Offhand" -> "Offhand",
    "Armor" -> "Armor",
    "Accessory" -> "Accessory",
    "Accessory" -> "Accessory",
    // status menu
    "LVL" -> "LVL",
    "HP" -> "HP",
    "MP" -> "MP",
    // store menu
    "Gold:" -> "Gold:",
    "Owned:" -> "Owned:",
    "Buy" -> "Buy",
    "Sell" -> "Sell")
}
