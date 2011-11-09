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
    // some utility functions
    def idx(x: Int, y: Int) = dataIndex(x, y, mapMeta.xSize)
    def isAutotile(x: Int, y: Int) = layerAry(idx(x, y)) == autotileByte
    def getAutotileNum(x: Int, y: Int) = layerAry(idx(x, y)+1) & 0xff
    def findLast(stopPredicate: IntVec => Boolean, begin: IntVec, dir: Int) =
    {
      val dirOffset = offsets(dir)
      @tailrec def testTile(cur: IntVec) : IntVec = {
        val nextTile = cur+dirOffset
        // stop if next is out of bounds, non autotile, or diferent autotile
        if(stopPredicate(nextTile)) cur else testTile(nextTile)
      }
      testTile(begin)
    }
    
    // Only need to adjust autotiles in this rect
    val initialSeqOfTiles =
      for(x <- x0-1 to x1+1;
          y <- y0-1 to y1+1;
          if withinBounds(mapMeta, x, y) && isAutotile(x,y)) yield (x, y)
          
    // assume all tiles in set are within bounds
    @tailrec def setFirstTile(tilesRemainingToSet: Seq[(Int, Int)],
                              accumRect: Rectangle = NilRect()) : Rectangle = 
    {
      if(tilesRemainingToSet.isEmpty) accumRect else {
        val (xToSet, yToSet) = tilesRemainingToSet.head
        
        val autotileNum = getAutotileNum(xToSet, yToSet)
        def sameType(x: Int, y: Int) =
          isAutotile(x, y) && getAutotileNum(x, y) == autotileNum
        def diffTypeOutsideSame(v: IntVec) =
          withinBounds(mapMeta, v.x, v.y) && !sameType(v.x, v.y)
        def diffTypeOutsideDiff(v: IntVec) =
          !withinBounds(mapMeta, v.x, v.y) || !sameType(v.x, v.y)
        
        // MUTABLE
        def mask(tileI: Int, maskInt: Int) =
          layerAry.update(tileI+2, maskInt.asInstanceOf[Byte])
        
        if(autotiles.length > autotileNum) {
          val autotile = autotiles(autotileNum)
          
          if(autotile.terrainMode) {
            // NOTE: In terrain mode, an out-of-bound tile counts as the 
            // same type tile.
            
            // This is easy. Just iterate through directions, setting flags
            // Mutable is more readable in this case
            var newConfig = 0
            for((mask, (dx, dy)) <- Autotile.DirectionMasks.offsets;
                if diffTypeOutsideSame(xToSet+dx, yToSet+dy))
              newConfig = newConfig | mask
            
            mask(idx(xToSet,yToSet), newConfig)
            setFirstTile(tilesRemainingToSet.tail, 
              accumRect union tileRect(xToSet, yToSet))
          } else {
            val findVertBounds = findLast(
              diffTypeOutsideDiff, (xToSet,yToSet), _: Int)
            
            val wallNorth = findVertBounds(NORTH).y
            val wallSouth = findVertBounds(SOUTH).y
            
            // either find different tile, or run into larger wall
            def wallBoundary(tileVec: IntVec) = 
              diffTypeOutsideDiff(tileVec) || 
              !diffTypeOutsideDiff(tileVec+offsets(NORTH))
            
            val findWallBoundary = 
              findLast(wallBoundary, (xToSet, wallNorth), _: Int)
            
            val wallWest  = findWallBoundary(WEST).x
            val wallEast  = findWallBoundary(EAST).x
            
            val wallXBounds = wallWest to wallEast
            val wallYBounds = wallNorth to wallSouth
            
            // determine if all tiles (xTest, y) 
            // where y elem [wallNorth, wallSouth] are same type autotile
            def allSameType(xTest: Int) = {
              // all same for out of bounds
              !withinBounds(mapMeta, xTest, wallNorth) || 
              wallYBounds.map(y => (xTest, y)).forall(
                t => sameType(t._1, t._2))
            }
            val inferiorWest = allSameType(wallWest-1)
            val inferiorEast = allSameType(wallEast+1)
            
            for(xInWall <- wallXBounds; yInWall <- wallYBounds;
                if sameType(xInWall, yInWall)) 
            {
              var newConfig = 0
              if(!inferiorWest && xInWall == wallWest)  newConfig |= WEST
              if(!inferiorEast && xInWall == wallEast)  newConfig |= EAST
              if(yInWall == wallNorth) newConfig |= NORTH
              if(yInWall == wallSouth) newConfig |= SOUTH
              mask(idx(xInWall, yInWall), newConfig)
            }
            
            // only choose tiles that are not in the wall we just calculated
            val remaining = tilesRemainingToSet.tail.filterNot {
              case (xRem, yRem) => 
                wallXBounds.contains(xRem) && wallYBounds.contains(yRem)
            }
            
            val wallRect = tileRect(
              wallEast, wallNorth, wallXBounds.size, wallYBounds.size)
            println(wallRect)
                       
            setFirstTile(remaining, accumRect union wallRect)
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
