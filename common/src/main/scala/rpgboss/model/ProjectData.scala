package rpgboss.model
import rpgboss.model.resource.RpgMap
import scala.collection.mutable.ArrayBuffer

case class ProjectDataStartup(
  var startingLoc: MapLoc = MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
  var startingParty: ArrayBuffer[Int] = ArrayBuffer(0),

  var titlePic: String = "LordSpirit.jpg",
  var titleMusic: Option[SoundSpec] = None,

  var windowskin: String = "LastPhantasmScanlines.png",
  var msgfont: String = "VeraMono.ttf",
  var fontsize: Int = 24,

  var soundCursor: Option[SoundSpec] = Some(SoundSpec("MenuCursor.wav")),
  var soundSelect: Option[SoundSpec] = Some(SoundSpec("MenuSelect.wav")),
  var soundCancel: Option[SoundSpec] = Some(SoundSpec("MenuCancel.wav")),
  var soundCannot: Option[SoundSpec] = Some(SoundSpec("MenuCannot.wav")))

case class ProjectDataEnums(
  var characters: ArrayBuffer[Character] = ProjectData.defaultCharacters,
  var classes: ArrayBuffer[CharClass] = ArrayBuffer(CharClass()),
  var statusEffects: ArrayBuffer[StatusEffect] = ArrayBuffer(StatusEffect()),
  var items: ArrayBuffer[Item] = ArrayBuffer(Item()),
  var skills: ArrayBuffer[Skill] = ArrayBuffer(Skill()),
  var elements: ArrayBuffer[String] = ArrayBuffer(""),
  var equipSubtypes: ArrayBuffer[String] = ArrayBuffer(""))

case class ProjectData(
  var uuid: String,
  var title: String,
  var recentMapName: String = "",
  var lastCreatedMapId: Int = 1, // Start at 1)
  var startup: ProjectDataStartup = ProjectDataStartup(),
  var enums: ProjectDataEnums = ProjectDataEnums()) {
}

object ProjectData {
  def defaultCharacters = ArrayBuffer(
    Character("Pando", sprite = Some(SpriteSpec("vx_chara01_a.png", 4))),
    Character("Estine", sprite = Some(SpriteSpec("vx_chara01_a.png", 1))),
    Character("Leoge", sprite = Some(SpriteSpec("vx_chara01_a.png", 3))),
    Character("Graven", sprite = Some(SpriteSpec("vx_chara01_a.png", 2))),
    Character("Carona", sprite = Some(SpriteSpec("vx_chara01_a.png", 6))))

  /*
  def defaultDamageTypes = ArrayBuffer(
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
  )*/
}
