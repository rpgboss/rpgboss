package rpgboss.model
import rpgboss.model.resource.RpgMap

case class ProjectDataStartup(
  var startingLoc: MapLoc = MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
  var startingParty: Seq[Int] = Seq(0),

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
  var characters: Seq[Character] = ProjectData.defaultCharacters,
  var classes: Seq[CharClass] = Seq(CharClass()),
  var enemies: Seq[Enemy] = Seq(Enemy()),
  var statusEffects: Seq[StatusEffect] = Seq(StatusEffect()),
  var items: Seq[Item] = Seq(Item()),
  var skills: Seq[Skill] = Seq(Skill()),
  var elements: Seq[String] = Seq(""),
  var equipSubtypes: Seq[String] = Seq(""))

case class ProjectData(
  var uuid: String,
  var title: String,
  var recentMapName: String = "",
  var lastCreatedMapId: Int = 1, // Start at 1)
  var startup: ProjectDataStartup = ProjectDataStartup(),
  var enums: ProjectDataEnums = ProjectDataEnums()) {
}

object ProjectData {
  def defaultCharacters = Seq(
    Character("Pando", sprite = Some(SpriteSpec("vx_chara01_a.png", 4))),
    Character("Estine", sprite = Some(SpriteSpec("vx_chara01_a.png", 1))),
    Character("Leoge", sprite = Some(SpriteSpec("vx_chara01_a.png", 3))),
    Character("Graven", sprite = Some(SpriteSpec("vx_chara01_a.png", 2))),
    Character("Carona", sprite = Some(SpriteSpec("vx_chara01_a.png", 6))))

  /*
  def defaultDamageTypes = Seq(
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
