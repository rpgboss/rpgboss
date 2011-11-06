package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._
import rpgboss.model._
import rpgboss.model.RpgMap._

import scala.annotation.tailrec

import java.awt.Rectangle

trait MapViewTool {
  def name: String
  override def toString = name
  def onMousePressed(vs: MapViewState, tCodes: Array[Array[Array[Byte]]], 
                     x1: Int, y1: Int) : Rectangle
  // x1, y1 are init press coords; x2, y2 are where mouse has been dragged 
  def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                     x1: Int, y1: Int, x2: Int, y2: Int) : Rectangle
  
  def selectionSqOnDrag: Boolean = true
}

object MapViewTools extends ListedEnum[MapViewTool] {
  
  def withinBounds(mapMeta: RpgMap, x: Int, y: Int) = {
    x < mapMeta.xSize && y < mapMeta.ySize && x >= 0 && y >= 0 
  }
  
  def setAutotileFlags(mapMeta: RpgMap, autotiles: Vector[Autotile],
                       layerAry: Array[Byte],  
                       x0: Int, y0: Int, x1: Int, y1: Int) = 
  {
    val initialSeqOfTiles =
      for(x <- x0-1 to x1+1;
          y <- y0-1 to y1+1;
          if withinBounds(mapMeta, x, y)) yield (x, y)
    
    def idx(x: Int, y: Int) = dataIndex(x, y, mapMeta.xSize)
          
    // assume all tiles in set are within bounds
    @tailrec def setFirstTile(tilesRemainingToSet: Seq[(Int, Int)],
                              accumRect: Rectangle = NilRect()) : Rectangle = 
    {
      if(!tilesRemainingToSet.isEmpty) {
        val (x, y) = tilesRemainingToSet.head
        def getAutotileNum(x: Int, y: Int) = layerAry(idx(x, y)+1) & 0xff
        
        val autotileNum = getAutotileNum(x, y)
        
        if(autotiles.length > autotileNum) {
          val autotile = autotiles(autotileNum)
          
          if(autotile.terrainMode) {
            // easy. Just iterate through directions, setting flags
            // Mutable is more readable in this case
            var newConfig = 0
            for((mask, (dx, dy)) <- Autotile.DirectionMasks.offsets;
                if withinBounds(mapMeta, x+dx, y+dy);
                if getAutotileNum(x+dx, y+dy) != autotileNum)
              newConfig = newConfig | mask
            
            layerAry.update(idx(x,y)+2, newConfig.asInstanceOf[Byte])
            setFirstTile(
              tilesRemainingToSet.tail, accumRect union tileRect(x, y))
          } else {
            println("No terrain")
            setFirstTile(tilesRemainingToSet.tail, accumRect)
          }
          
        } else setFirstTile(tilesRemainingToSet.tail, accumRect) // do nothing
      } else accumRect
    }
    
    setFirstTile(initialSeqOfTiles)
  }
  
  case object Pencil extends MapViewTool {
    val name = "Pencil"
    def onMousePressed(vs, tCodes, x1, y1) = 
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
            (tCode, xi) <- tileRow.zipWithIndex)
        {
          val x = x1+xi
          val y = y1+yi
          
          if(withinBounds(vs.mapMeta, x, y)) {
            val i = dataIndex(x, y, vs.mapMeta.xSize)
            println("Modified tile: (%d, %d)".format(x, y))
            for(j <- 0 until bytesPerTile) 
              layerAry.update(i+j, tCode(j)) 
          }
        }
        
        val directlyEditedRect = 
          tileRect(x1, y1, tCodes(0).length, tCodes.length)
        
        val autotileRect =
          setAutotileFlags(vs.mapMeta, vs.sm.autotiles, layerAry, 
                           x1, y1, x1+tCodes(0).length-1, y1+tCodes.length-1)
        
        println(directlyEditedRect union autotileRect)
          
        directlyEditedRect union autotileRect
      } getOrElse {
        NilRect()
      }
    }
    def onMouseDragged(vs, tCodes, x1, y1, x2, y2) = 
    {
      onMousePressed(vs, tCodes, x2, y2) // no difference
    }
  }
  
  val valueList = List(Pencil)
  selected = Pencil
}
