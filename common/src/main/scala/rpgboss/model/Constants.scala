package rpgboss.model

trait RpgEnum extends Enumeration {
  def findOrDefault(s: String) = {
    values.find(_.toString == s).getOrElse(default)
  }
  def default: Value
}

object Constants {

  object DirectionMasks {
    val NORTH = 1 << 0
    val EAST  = 1 << 1
    val SOUTH = 1 << 2
    val WEST  = 1 << 3
    val NE    = 1 << 4
    val SE    = 1 << 5
    val SW    = 1 << 6
    val NW    = 1 << 7
    
    val ALLCARDINAL = NORTH|EAST|SOUTH|WEST
    val NONE = 0
    
    def allPassable(b: Byte) = (b & ALLCARDINAL) == 0
    def allBlocked(b: Byte)  = (b & ALLCARDINAL) == ALLCARDINAL
    def someBlocked(b: Byte) = (b & ALLCARDINAL) > 0
  }
  
  import DirectionMasks._
  
  val DirectionOffsets = Map(
    NORTH->(0, -1),
    EAST ->(1, 0),
    SOUTH->(0, 1),
    WEST ->(-1, 0),
    NE   ->(1, -1),
    SE   ->(1, 1),
    SW   ->(-1, 1),
    NW   ->(-1, -1)
  )
  
  object Transitions extends RpgEnum {
    val NONE = Value(0, "None")
    val FADE = Value(1, "Fade out")
    
    def default = FADE
    
    val fadeLength = 500
  }
  
  object Scope extends RpgEnum {
    val None = Value
    val OneEnemy, AllEnemies = Value
    val OneAlly, AllAllies = Value
    val OneAllyDead, AllAlliesDead = Value
    val SelfOnly = Value
    
    def default = OneAlly
  }
  
  object ItemType extends RpgEnum {
    val Consumable, Rare, Equipment = Value
    
    def default = Consumable
  }
  
  object EquipSlot extends RpgEnum {
    val None, Weapon, Offhand, Armor, Head, Accessory = Value
    def default = None
  }
}