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
    Some(SoundSpec("rpgboss-menu/MenuCursor.wav")),
  var soundSelect: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuSelect.wav")),
  var soundCancel: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuCancel.wav")),
  var soundCannot: Option[SoundSpec] =
    Some(SoundSpec("rpgboss-menu/MenuCannot.wav")))

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
  var uuid: String,
  var title: String,
  var recentMapName: String = "",
  var lastCreatedMapId: Int = 1, // Start at 1)
  var startup: ProjectDataStartup = ProjectDataStartup(),
  var enums: ProjectDataEnums = ProjectDataEnums()) {

  def write(dir: File) =
    JsonUtils.writeModelToJson(ProjectData.rootFile(dir), this)
}

object ProjectData {
  def rootFile(dir: File) = new File(dir, "rpgproject.json")

  def read(dir: File) = JsonUtils.readModelFromJson[ProjectData](rootFile(dir))

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
