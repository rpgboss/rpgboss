package rpgboss.editor.lib
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import java.awt.Rectangle
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color

object TileUtils {

  /**
   * Draw every autotile onto collageImage in one huge row.
   */
  def getAutotileCollageImg(autotiles: Array[Autotile]) = {
    import Tileset.tilesize
    val collageImage = new BufferedImage(autotiles.length * tilesize,
      tilesize,
      BufferedImage.TYPE_4BYTE_ABGR)

    val g = collageImage.createGraphics()

    autotiles.zipWithIndex map {
      case (autotile, i) =>
        g.drawImage(autotile.isolatedImg, i * tilesize, 0, null)
    }

    collageImage
  }

  /**
   * Computes essential information on which tiles need to be redrawn
   */
  def getTileBounds(
    bounds: Rectangle,
    tilesizeX: Int,
    tilesizeY: Int,
    nXTiles: Int,
    nYTiles: Int) = {
    val minX = bounds.getMinX / tilesizeX
    val maxX = bounds.getMaxX / tilesizeX
    val minY = bounds.getMinY / tilesizeY
    val maxY = bounds.getMaxY / tilesizeY

    val minXTile = minX.toInt
    val minYTile = minY.toInt

    val maxXTile = math.min(nXTiles - 1, maxX.toInt)
    val maxYTile = math.min(nYTiles - 1, maxY.toInt)

    (minX, minY, maxX, maxY, minXTile, minYTile, maxXTile, maxYTile)
  }

  def drawGrid(
    gArg: Graphics2D, tilesizeX: Int, tilesizeY: Int,
    minXTile: Int, minYTile: Int, maxXTile: Int, maxYTile: Int) = {

    val g = gArg.create().asInstanceOf[Graphics2D]

    g.setStroke(new BasicStroke(2.0f))
    g.setColor(new Color(0f, 0f, 0f, 0.3f))

    // Draw vertical lines
    for (xTile <- minXTile to maxXTile + 1) {
      val x = xTile * tilesizeX
      g.draw(
        new Line2D.Double(x, minYTile * tilesizeY, x, (maxYTile + 1) * tilesizeY))
    }

    // Draw horizontal lines
    for (yTile <- minYTile to maxYTile + 1) {
      val y = yTile * tilesizeY
      g.draw(
        new Line2D.Double(minXTile * tilesizeX, y, (maxXTile + 1) * tilesizeX, y))
    }
  }
}