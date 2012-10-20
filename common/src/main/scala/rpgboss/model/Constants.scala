package rpgboss.model

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
}