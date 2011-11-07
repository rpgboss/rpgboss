package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._
import rpgboss.model._
import rpgboss.model.Autotile.DirectionMasks._
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

    // some utility functions
    def idx(x: Int, y: Int) = dataIndex(x, y, mapMeta.xSize)
    def isAutotile(x: Int, y: Int) = layerAry(idx(x, y)) == autotileByte
    def getAutotileNum(x: Int, y: Int) = layerAry(idx(x, y)+1) & 0xff
    def findLastAutotile(autotileNum: Int, begin: IntVec, dir: Int) =
    {
      val dirOffset = offsets(dir)
      @tailrec def testTile(cur: IntVec) : IntVec = {
        val nextTile = cur+dirOffset
        // stop if next is out of bounds, non autotile, or diferent autotile
        if( !withinBounds(mapMeta, nextTile.x, nextTile.y) ||
            !isAutotile(nextTile.x, nextTile.y) ||
            getAutotileNum(nextTile.x, nextTile.y) != autotileNum )
          cur 
        else 
          testTile(nextTile)
      }
      testTile(begin)
    }
          
    // assume all tiles in set are within bounds
    @tailrec def setFirstTile(tilesRemainingToSet: Seq[(Int, Int)],
                              accumRect: Rectangle = NilRect()) : Rectangle = 
    {
      if(tilesRemainingToSet.isEmpty) accumRect else {
        val (x, y) = tilesRemainingToSet.head
        
        val autotileNum = getAutotileNum(x, y)
        def sameType(xTest: Int, yTest: Int) = 
          getAutotileNum(xTest, yTest) == autotileNum        
        def mask(tileI: Int, maskInt: Int) =
          layerAry.update(tileI+2, maskInt.asInstanceOf[Byte])
        
        if(autotiles.length > autotileNum) {
          val autotile = autotiles(autotileNum)
          
          if(autotile.terrainMode) {
            // easy. Just iterate through directions, setting flags
            // Mutable is more readable in this case
            var newConfig = 0
            for((mask, (dx, dy)) <- Autotile.DirectionMasks.offsets;
                if withinBounds(mapMeta, x+dx, y+dy);
                if !sameType(x+dx, y+dy))
              newConfig = newConfig | mask
            
            mask(idx(x,y), newConfig)
            setFirstTile(
              tilesRemainingToSet.tail, accumRect union tileRect(x, y))
          } else {
            val findLast = findLastAutotile(autotileNum, _: IntVec, _: Int)
            val wallNorth = findLast((x,y), NORTH).y
            val wallSouth = findLast((x,y), SOUTH).y
            val wallWest  = findLast((x, wallNorth), WEST).x
            val wallEast  = findLast((x, wallNorth), EAST).x
            
            for(xInWall <- wallWest to wallEast;
                yInWall <- wallNorth to wallSouth;
                if sameType(xInWall, yInWall)) 
            {
              var newConfig = 0
              if(xInWall == wallWest)  newConfig |= WEST
              if(xInWall == wallEast)  newConfig |= EAST
              if(yInWall == wallNorth) newConfig |= NORTH
              if(yInWall == wallSouth) newConfig |= SOUTH
              mask(idx(xInWall, yInWall), newConfig)
            }
            
            // only choose tiles that are not in the wall we just calculated
            val remaining = tilesRemainingToSet.tail.filterNot {
              case (xRem, yRem) => 
                xRem >= wallWest && xRem <= wallEast && 
                yRem >= wallNorth && yRem <= wallSouth
            }
            
            val newRect = accumRect union tileRect(
              wallEast, wallNorth, wallWest-wallEast+1, wallSouth-wallNorth+1)
                       
            setFirstTile(remaining, newRect)
          }
          
        } else setFirstTile(tilesRemainingToSet.tail, accumRect) // do nothing
      }
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
