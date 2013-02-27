package rpgboss.model
import rpgboss.model.resource.RpgMap
                 
case class ProjectData(uuid: String,
                       title: String, 
                       recentMapName: String = "",
                       lastCreatedMapId: Int = 1, // Start at 1
                       startingLoc: MapLoc = 
                         MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
                       
                       characters: Array[Character] = 
                         ProjectData.defaultCharacters,
                       
                       classes: Array[CharClass] = Array(CharClass()),
                       
                       titlePic: String = "LordSpirit.jpg",
                       startingParty: Array[Int] = Array(0),
                       
                       windowskin: String = "LastPhantasmScanlines.png",
                       msgfont: String = "Vera.ttf",
                       fontsize: Int = 24,
                       soundCursor: String = "MenuCursor.wav",
                       soundSelect: String = "MenuSelect.wav",
                       soundCancel: String = "MenuCancel.wav",
                       soundCannot: String = "MenuCannot.wav",
                       
                       statusEffects: Array[StatusEffect] = 
                         Array(StatusEffect()),
                       
                       items: Array[Item] = Array(Item()),
                       
                       skills: Array[Skill] = Array(Skill()),
                       
                       elements: Array[String] = Array(""),
                       equipSubtypes: Array[String] = Array("")
                       )

object ProjectData {
  def defaultCharacters = Array(
    Character("Pando",  sprite = Some(SpriteSpec("vx_chara01_a.png", 4))),  
    Character("Estine", sprite = Some(SpriteSpec("vx_chara01_a.png", 1))),
    Character("Leoge",  sprite = Some(SpriteSpec("vx_chara01_a.png", 3))),
    Character("Graven", sprite = Some(SpriteSpec("vx_chara01_a.png", 2))),
    Character("Carona", sprite = Some(SpriteSpec("vx_chara01_a.png", 6)))
  )
  
  /*
  def defaultDamageTypes = Array(
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
