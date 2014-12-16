package rpgboss.editor

import rpgboss.lib.Utils._
import rpgboss.editor.uibase._
import rpgboss.editor.misc.GraphicsUtils._
import rpgboss.model._
import rpgboss.model.DirectionMasks._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.model.resource.RpgMap._
import scala.annotation.tailrec
import java.awt.Rectangle

trait MapViewTool {
  def name: String
  override def toString = name
  def onMouseDown(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                  layer: MapLayers.Value,
                  x1: Int, y1: Int): TileRect
  // x1, y1 are init press coords; x2, y2 are where mouse has been dragged
  def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                     layer: MapLayers.Value,
                     x1: Int, y1: Int, x2: Int, y2: Int): TileRect

  def onMouseUp(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                layer: MapLayers.Value,
                x1: Int, y1: Int, x2: Int, y2: Int): TileRect = TileRect.empty

  def selectionSqOnDrag: Boolean = true
}

object MapViewTools {

  /**
   *
   */
  def setAutotileFlags(mapMeta: RpgMapMetadata, autotiles: Seq[Autotile],
                       layerAry: Array[Array[Byte]],
                       x0: Int, y0: Int, x1: Int, y1: Int): TileRect = {

    // Only need to adjust autotiles in this rect
    val initialSeqOfTiles =
      for (
        x <- x0 to x1;
        y <- y0 to y1
      ) yield (x, y)

    setAutotileFlags(mapMeta, autotiles, layerAry, initialSeqOfTiles)
  }

  /**
   * Frankly I don't have a damn clue how this works. I wish I commented it.
   * Well. This algorithm isn't even really correct for walls. It seems to mess
   * up in some cases.
   */
  def setAutotileFlags(mapMeta: RpgMapMetadata, autotiles: Seq[Autotile],
                       layerAry: Array[Array[Byte]],
                       tilesToSet: Seq[(Int, Int)]): TileRect =
    {
      import RpgMap.bytesPerTile
      // some utility functions
      def isAutotile(x: Int, y: Int) = layerAry(y)(x * bytesPerTile) == autotileByte
      def getAutotileNum(x: Int, y: Int) = layerAry(y)(x * bytesPerTile + 1) & 0xff
      def findLast(stopPredicate: IntVec => Boolean, begin: IntVec, dir: Int) =
        {
          val dirOffset = DirectionOffsets(dir)
          @tailrec def testTile(cur: IntVec): IntVec = {
            val nextTile = cur + dirOffset
            // stop if next is out of bounds, non autotile, or diferent autotile
            if (stopPredicate(nextTile)) cur else testTile(nextTile)
          }
          testTile(begin)
        }

      val visited = new collection.mutable.HashSet[(Int, Int)]()
      val queue = new collection.mutable.Queue[(Int, Int)]()
      var modifiedRect = TileRect.empty

      queue.enqueue(tilesToSet: _*)

      while (!queue.isEmpty) {
        val curTile = queue.dequeue()
        if (!visited.contains(curTile)) {
          visited.add(curTile)
          val (xToSet, yToSet) = curTile

          if (mapMeta.withinBounds(xToSet, yToSet) && isAutotile(xToSet, yToSet)) {

            val autotileNum = getAutotileNum(xToSet, yToSet)
            def sameType(x: Int, y: Int) =
              isAutotile(x, y) && getAutotileNum(x, y) == autotileNum
            def diffTypeOutsideSame(v: IntVec) =
              mapMeta.withinBounds(v.x, v.y) && !sameType(v.x, v.y)
            def diffTypeOutsideDiff(v: IntVec) =
              !mapMeta.withinBounds(v.x, v.y) || !sameType(v.x, v.y)

            // MUTABLE
            def mask(xToSet: Int, yToSet: Int, maskInt: Int) =
              layerAry(yToSet).update(
                xToSet * bytesPerTile + 2, maskInt.asInstanceOf[Byte])

            if (autotiles.length > autotileNum) {
              val autotile = autotiles(autotileNum)

              if (autotile.terrainMode) {
                // NOTE: In terrain mode, an out-of-bound tile counts as the
                // same type tile.

                // This is easy. Just iterate through directions, setting flags
                // Mutable is more readable in this case
                var newConfig = 0
                for (
                  (mask, (dx, dy)) <- DirectionOffsets;
                  if diffTypeOutsideSame(xToSet + dx, yToSet + dy)
                ) newConfig = newConfig | mask

                mask(xToSet, yToSet, newConfig)

                modifiedRect = modifiedRect | TileRect(xToSet, yToSet)
              } else {
                val findVertBounds = findLast(
                  diffTypeOutsideDiff, (xToSet, yToSet), _: Int)

                val wallNorth = findVertBounds(NORTH).y
                val wallSouth = findVertBounds(SOUTH).y

                // either find different tile, or run into larger wall
                def wallBoundary(tileVec: IntVec) =
                  diffTypeOutsideDiff(tileVec) ||
                    !diffTypeOutsideDiff(tileVec + DirectionOffsets(NORTH))

                val findWallBoundary =
                  findLast(wallBoundary, (xToSet, wallNorth), _: Int)

                val wallWest = findWallBoundary(WEST).x
                val wallEast = findWallBoundary(EAST).x

                val wallXBounds = wallWest to wallEast
                val wallYBounds = wallNorth to wallSouth

                // determine if all tiles (xTest, y)
                // where y elem [wallNorth, wallSouth] are same type autotile
                def allSameType(xTest: Int) = {
                  // all same for out of bounds
                  !mapMeta.withinBounds(xTest, wallNorth) ||
                    wallYBounds.map(y => (xTest, y)).forall(
                      t => sameType(t._1, t._2))
                }
                val inferiorWest = allSameType(wallWest - 1)
                val inferiorEast = allSameType(wallEast + 1)

                for (
                  xInWall <- wallXBounds; yInWall <- wallYBounds;
                  if sameType(xInWall, yInWall)
                ) {
                  var newConfig = 0
                  if (!inferiorWest && xInWall == wallWest) newConfig |= WEST
                  if (!inferiorEast && xInWall == wallEast) newConfig |= EAST
                  if (yInWall == wallNorth) newConfig |= NORTH
                  if (yInWall == wallSouth) newConfig |= SOUTH
                  mask(xInWall, yInWall, newConfig)
                }

                // Remove tiles from queue that are in the wall we just calculated
                queue.dequeueAll({
                  case (xRem, yRem) =>
                    wallXBounds.contains(xRem) && wallYBounds.contains(yRem)
                })

                val wallRect = TileRect(
                  wallEast, wallNorth, wallXBounds.size, wallYBounds.size)

                modifiedRect = modifiedRect | wallRect
              }
            }
          }
        }
      }

      modifiedRect
    }

  case object Pencil extends MapViewTool {
    val name = "Pencil"
    def onMouseDown(
      vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
      layer: MapLayers.Value,
      x1: Int, y1: Int) = {
      import MapLayers._

      mapOfArrays(vs.nextMapData).get(layer).map { layerAry =>
        for (
          (tileRow, yi) <- tCodes.zipWithIndex;
          (tCode, xi) <- tileRow.zipWithIndex
        ) {
          val x = x1 + xi
          val y = y1 + yi

          if (vs.mapMeta.withinBounds(x, y)) {
            println("Modified tile: (%d, %d)".format(x, y))
            for (j <- 0 until bytesPerTile)
              layerAry(y).update(x * bytesPerTile + j, tCode(j))
          }
        }

        val directlyEditedRect =
          TileRect(x1, y1, tCodes(0).length, tCodes.length)

        val autotileRect =
          setAutotileFlags(vs.mapMeta, vs.tileCache.autotiles, layerAry,
            x1 - 1, y1 - 1, x1 + tCodes(0).length, y1 + tCodes.length)

        directlyEditedRect | autotileRect
      } getOrElse {
        TileRect.empty
      }
    }
    def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                       layer: MapLayers.Value,
                       x1: Int, y1: Int, x2: Int, y2: Int) = {
      onMouseDown(vs, tCodes, layer, x2, y2) // no difference
    }
  }

  trait RectLikeTool extends MapViewTool {
    var origLayerBuf: Array[Array[Byte]] = null
    var prevPaintedRegion = TileRect.empty

    def doesPaint(xi: Int, yi: Int, w: Int, h: Int): Boolean

    def doPaint(
      vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
      layerAry: Array[Array[Byte]],
      x1: Int, y1: Int, x2: Int, y2: Int): TileRect =
      {
        println("Modified tiles: (%d, %d) to (%d, %d)".format(x1, y1, x2, y2))

        import math._
        val xMin = min(x1, x2)
        val xMax = max(x1, x2)
        val yMin = min(y1, y2)
        val yMax = max(y1, y2)

        val w = xMax - xMin + 1
        val h = yMax - yMin + 1

        for (xi <- 0 until w; yi <- 0 until h) {
          val x = xMin + xi
          val y = yMin + yi

          if (vs.mapMeta.withinBounds(x, y) && doesPaint(xi, yi, w, h)) {
            val tCodeY = yi % tCodes.length
            val tCodeX = xi % tCodes.head.length

            val tCode = tCodes(tCodeY)(tCodeX)

            for (j <- 0 until bytesPerTile) {
              layerAry(y).update(x * bytesPerTile + j, tCode(j))
            }
          }
        }

        val directlyEditedRect =
          TileRect(xMin, yMin, w, h)

        val autotileRect =
          setAutotileFlags(vs.mapMeta, vs.tileCache.autotiles, layerAry,
            xMin - 1, yMin - 1, xMax + 1, yMax + 1)

        directlyEditedRect | autotileRect
      }

    def onMouseDown(
      vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
      layer: MapLayers.Value,
      x1: Int, y1: Int) = {
      import MapLayers._

      mapOfArrays(vs.nextMapData).get(layer).map { layerAry =>
        // Stash the original tile layer
        origLayerBuf = layerAry.map(_.clone())

        doPaint(vs, tCodes, layerAry, x1, y1, x1, y1)
      } getOrElse {
        TileRect.empty
      }
    }

    def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                       layer: MapLayers.Value,
                       x1: Int, y1: Int, x2: Int, y2: Int) = {
      import MapLayers._

      mapOfArrays(vs.nextMapData).get(layer).map { layerAry =>
        // restore original layer to nextMapData
        for (i <- 0 until layerAry.size) {
          layerAry.update(i, origLayerBuf(i).clone())
        }

        val newRegion = doPaint(vs, tCodes, layerAry, x1, y1, x2, y2)
        val totalNeedToRepaint = newRegion | prevPaintedRegion

        prevPaintedRegion = newRegion

        totalNeedToRepaint
      } getOrElse {
        TileRect.empty
      }
    }
  }

  case object Rectangle extends RectLikeTool {
    val name = "Rectangle"

    def doesPaint(xi: Int, yi: Int, w: Int, h: Int) = true
  }

  case object Ellipse extends RectLikeTool {
    val name = "Ellipse"

    def doesPaint(xi: Int, yi: Int, w: Int, h: Int) = {
      // Calculate whether or not to paint this tile given the above params
      val a = w.toDouble / 2
      val b = h.toDouble / 2

      import math._

      // fudge a bit to make smaller elipses less rectangle
      val factor = if (w > 7 && h > 7) 1.0 else 0.88

      pow((xi + 0.5 - a) / a, 2) + pow((yi + 0.5 - b) / b, 2) < factor
    }
  }

  /**
   * Defines a flood fill bucket filler
   */
  case object Bucket extends MapViewTool {
    val name = "Bucket"
    def onMouseDown(
      vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
      layer: MapLayers.Value,
      x1: Int, y1: Int) = {
      import MapLayers._

      val visited = new collection.mutable.HashSet[(Int, Int)]()
      val needAutotileFlagsSet = new collection.mutable.HashSet[(Int, Int)]()

      val queue = new collection.mutable.Queue[(Int, Int)]()
      queue.enqueue((x1, y1))

      // Running set of variables defining bounds of modified tiles
      var minX, maxX = x1
      var minY, maxY = y1

      mapOfArrays(vs.nextMapData).get(layer).map { layerAry =>

        /*
         * Defines the matching condition we use for tiles we walk to.
         * For autotiles, only match first two bytes, since the last byte is
         * used for the boundary configuration.
         * For blank tiles, match any.
         */
        val matchesSeedF: (Byte, Byte, Byte) => Boolean = {
          import RpgMap._

          val b1Orig = layerAry(y1)(x1 * bytesPerTile)
          val b2Orig = layerAry(y1)(x1 * bytesPerTile + 1)
          val b3Orig = layerAry(y1)(x1 * bytesPerTile + 2)

          def match1(b1: Byte, b2: Byte, b3: Byte) =
            (b1 == b1Orig)
          def match2(b1: Byte, b2: Byte, b3: Byte) =
            (b1 == b1Orig) && (b2 == b2Orig)
          def match3(b1: Byte, b2: Byte, b3: Byte) =
            (b1 == b1Orig) && (b2 == b2Orig) && (b3 == b3Orig)

          b1Orig match {
            // Empty tile. Second two bytes are ignored
            case `emptyTileByte` => match1 _
            case `autotileByte` => match2 _
            case _ => match3 _
          }
        }

        // Iterate through the queue, doing a breadth first graph search
        while (!queue.isEmpty) {
          val curTile = queue.dequeue()

          // Make sure we haven't visited this tile already
          if (!visited.contains(curTile)) {
            visited.add(curTile)

            val (curX, curY) = curTile

            // Only process if it's within the bounds
            if (vs.mapMeta.withinBounds(curX, curY)) {

              val b1 = layerAry(curY)(curX * bytesPerTile)
              val b2 = layerAry(curY)(curX * bytesPerTile + 1)
              val b3 = layerAry(curY)(curX * bytesPerTile + 2)

              if (matchesSeedF(b1, b2, b3)) {
                // Gets the appropriate tile to fill in based on user selection
                val tCodeY = pmod(curX - x1, tCodes.length)
                val tCodeX = pmod(curY - y1, tCodes.head.length)
                val tCode = tCodes(tCodeY)(tCodeX)

                // Fill in the tile
                for (i <- 0 until bytesPerTile)
                  layerAry(curY)(curX * bytesPerTile + i) = tCode(i)

                // Update the running variables of min/max of edited tiles
                import math._
                minX = min(minX, curX)
                minY = min(minY, curY)
                maxX = max(maxX, curX)
                maxY = max(maxY, curY)

                // Add to set of autotiles that may need updating
                // self
                needAutotileFlagsSet.add(curTile)
                // NSEW
                needAutotileFlagsSet.add((curX - 1, curY))
                needAutotileFlagsSet.add((curX + 1, curY))
                needAutotileFlagsSet.add((curX, curY - 1))
                needAutotileFlagsSet.add((curX, curY + 1))
                // diagonal directions
                needAutotileFlagsSet.add((curX - 1, curY - 1))
                needAutotileFlagsSet.add((curX + 1, curY - 1))
                needAutotileFlagsSet.add((curX - 1, curY + 1))
                needAutotileFlagsSet.add((curX + 1, curY + 1))

                // Visit the neighboring tiles next
                queue.enqueue((curX - 1, curY))
                queue.enqueue((curX + 1, curY))
                queue.enqueue((curX, curY - 1))
                queue.enqueue((curX, curY + 1))
              }
            }
          }
        }

        // A rectangle with the min and max bounds defined
        val directlyEditedRect =
          TileRect(minX, minY, maxX - minX + 1, maxY - minY + 1)

        // A probably larger rect with the tiles with updated autotile flags
        val autotileRect =
          setAutotileFlags(vs.mapMeta, vs.tileCache.autotiles, layerAry,
            needAutotileFlagsSet.toSeq)

        directlyEditedRect | autotileRect
      } getOrElse {
        TileRect.empty
      }
    }
    def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                       layer: MapLayers.Value,
                       x1: Int, y1: Int, x2: Int, y2: Int) = {
      // Do nothing actually
      TileRect.empty
    }
  }

  case object Eraser extends MapViewTool {
    val name = "Eraser"

    def onMouseDown(
      vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
      layer: MapLayers.Value,
      x1: Int, y1: Int) = {
      import MapLayers._

      TileRect.empty
    }
    def onMouseDragged(vs: MapViewState, tCodes: Array[Array[Array[Byte]]],
                       layer: MapLayers.Value,
                       x1: Int, y1: Int, x2: Int, y2: Int) = {
      TileRect.empty
    }
  }
}

object MapViewToolsEnum extends RpgEnum {
  val Pencil, Rectangle, Ellipse, Bucket, Eraser = Value

  val toolMap = Map(
    Pencil -> MapViewTools.Pencil,
    Rectangle -> MapViewTools.Rectangle,
    Ellipse -> MapViewTools.Ellipse,
    Bucket -> MapViewTools.Bucket,
    Eraser -> MapViewTools.Eraser)

  def getTool(value: Value) = {
    toolMap.get(value).get
  }
  val valueList = List(Pencil, Rectangle, Ellipse, Bucket,Eraser)

  def default = Pencil
}
