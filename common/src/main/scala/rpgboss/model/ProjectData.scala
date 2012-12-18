package rpgboss.model
import rpgboss.model.resource.RpgMap
                 
case class ProjectData(title: String, 
                       recentMapName: String = "",
                       lastCreatedMapId: Int = 1, // Start at 1
                       startingLoc: MapLoc = 
                         MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
                       
                       characters: Array[Character] = 
                         ProjectData.defaultCharacters,
                       
                       titlePic: String = "LordSpirit.jpg",
                       startingParty: Array[Int] = Array(0),
                       
                       windowskin: String = "LastPhantasmScanlines.png",
                       msgfont: String = "Vera.ttf",
                       fontsize: Int = 24,
                       soundCursor: String = "MenuCursor.wav",
                       soundSelect: String = "MenuSelect.wav",
                       soundCancel: String = "MenuCancel.wav",
                       soundCannot: String = "MenuCannot.wav",
                       
                       charStates: Array[CharState] = 
                         CharState.defaultStates,
                       damageTypes: Array[String] = 
                         ProjectData.defaultDamageTypes,
                       itemTypes: Array[String] =
                         Item.defaultItemTypes,
                       items: Array[Item] =
                         Item.defaultItems
                       )

object ProjectData {
  def defaultCharacters = Array(
    Character("Pando", SpriteSpec("vx_chara01_a.png", 4)),  
    Character("Estine", SpriteSpec("vx_chara01_a.png", 1)),
    Character("Leoge", SpriteSpec("vx_chara01_a.png", 3)),
    Character("Graven", SpriteSpec("vx_chara01_a.png", 2)),
    Character("Carona", SpriteSpec("vx_chara01_a.png", 6))
  )
  
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
  )
}
