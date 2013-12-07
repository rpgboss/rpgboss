package rpgboss.model
import rpgboss.model.resource.RpgMap
import java.io.File
import rpgboss.lib.JsonUtils

case class ProjectDataStartup(
  var startingLoc: MapLoc = MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
  var startingParty: Seq[Int] = Seq(0),

  var titlePic: String = "LordSpirit.jpg",
  var titleMusic: Option[SoundSpec] = None,

  var windowskin: String = "LastPhantasmScanlines.png",
  var msgfont: String = "VeraMono.ttf",
  var fontsize: Int = 24,

  var soundCursor: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuCursor.mp3")),
  var soundSelect: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuSelect.mp3")),
  var soundCancel: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuCancel.mp3")),
  var soundCannot: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuCannot.mp3")))

case class ProjectDataEnums(
  var animations: Seq[Animation] = Seq(Animation()),
  var characters: Seq[Character] = ProjectData.defaultCharacters,
  var classes: Seq[CharClass] = Seq(CharClass()),
  var elements: Seq[String] = ProjectData.defaultElements,
  var enemies: Seq[Enemy] = Seq(Enemy()),
  var encounters: Seq[Encounter] = Seq(Encounter()),
  var equipTypes: Seq[String] = ProjectData.defaultEquipTypes,
  var items: Seq[Item] = Seq(Item()),
  var skills: Seq[Skill] = Seq(Skill()),
  var statusEffects: Seq[StatusEffect] = Seq(StatusEffect()))

case class ProjectData(
  var uuid: String = java.util.UUID.randomUUID().toString(),
  var title: String = "Untitled Project",
  var recentMapName: String = "",
  var lastCreatedMapId: Int = 1, // Start at 1)
  var startup: ProjectDataStartup = ProjectDataStartup(),
  var enums: ProjectDataEnums = ProjectDataEnums()) {

  def write(dir: File) = {
    val enumsStripped = copy(enums = null)
    def writeModel[T <: AnyRef](name: String, model: T) =
      JsonUtils.writeModelToJson(new File(dir, "%s.json".format(name)), model)

    writeModel("rpgproject", enumsStripped)
    writeModel("animations", enums.animations)
    writeModel("characters", enums.characters)
    writeModel("classes", enums.classes)
    writeModel("elements", enums.elements)
    writeModel("enemies", enums.enemies)
    writeModel("encounters", enums.encounters)
    writeModel("equipTypes", enums.equipTypes)
    writeModel("items", enums.items)
    writeModel("skills", enums.skills)
    writeModel("statusEffects", enums.statusEffects)
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
      readModel[Seq[Animation]]("animations", enums.animations = _)
      readModel[Seq[Character]]("characters", enums.characters = _)
      readModel[Seq[CharClass]]("classes", enums.classes = _)
      readModel[Seq[String]]("elements", enums.elements = _)
      readModel[Seq[Enemy]]("enemies", enums.enemies = _)
      readModel[Seq[Encounter]]("encounters", enums.encounters = _)
      readModel[Seq[String]]("equipTypes", enums.equipTypes = _)
      readModel[Seq[Item]]("items", enums.items = _)
      readModel[Seq[Skill]]("skills", enums.skills = _)
      readModel[Seq[StatusEffect]]("statusEffects", enums.statusEffects = _)
    }

    modelOpt
  }

  def defaultCharacters = Seq(
    Character("Pando", sprite = Some(SpriteSpec("vx_chara01_a.png", 4))),
    Character("Estine", sprite = Some(SpriteSpec("vx_chara01_a.png", 1))),
    Character("Leoge", sprite = Some(SpriteSpec("vx_chara01_a.png", 3))),
    Character("Graven", sprite = Some(SpriteSpec("vx_chara01_a.png", 2))),
    Character("Carona", sprite = Some(SpriteSpec("vx_chara01_a.png", 6))))

  def defaultElements = Seq(
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
    "Chaos"
  )

  def defaultEquipTypes = Seq(
    "Weapon",
    "Offhand",
    "Armor",
    "Head",
    "Accessory")
}
