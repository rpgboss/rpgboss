package rpgboss.rpgapplet

import rpgboss.rpgapplet.lib._
import rpgboss.model._
import java.awt.Rectangle

trait MapViewTool {
  def name: String
  override def toString = name
  def onMousePressed(vs: MapViewState, tCodes: Array[Array[Array[Byte]]], 
                     x: Int, y: Int) : Rectangle
  // x1, y1 are init press coords; x2, y2 are where mouse has been dragged 
  def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                     x1: Int, y1: Int, x2: Int, y2: Int) : Rectangle
  
  def selectionSqOnDrag: Boolean = true
}

object MapViewTools extends ListedEnum[MapViewTool] {
  case object Pencil extends MapViewTool {
    val name = "Pencil"
    def onMousePressed(vs, tCodes, x, y) = 
    {
      import MapLayers._
      
      val d = vs.mapData
      val layerOpt = MapLayers.selected match {
        case Bot => Some(d.botLayer)
        case Mid => Some(d.midLayer)
        case Top => Some(d.topLayer)
        case _ => None
      }
      
      layerOpt map { layerAry =>
        for((tileRow, yi) <- tCodes.zipWithIndex;
            (tCode, xi) <- tileRow.zipWithIndex;
            if x+xi < vs.mapMeta.xSize && y+yi < vs.mapMeta.ySize)
        {
          val i = RpgMap.dataIndex(x+xi, y+yi, vs.mapMeta.xSize)
          println("Modified tile: (%d, %d)".format(x+xi, y+yi))
          for(j <- 0 until RpgMap.bytesPerTile) 
            layerAry.update(i+j, tCode(j))
        }
      }
      
      GraphicsUtils.tileRect(x, y, tCodes(0).length, tCodes.length)
    }
    def onMouseDragged(vs, tCodes, x1, y1, x2, y2) = 
    {
      onMousePressed(vs, tCodes, x2, y2) // no difference
    }
  }
  
  val valueList = List(Pencil)
  selected = Pencil
}
