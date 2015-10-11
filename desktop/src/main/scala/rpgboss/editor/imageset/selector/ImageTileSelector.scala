package rpgboss.editor.imageset.selector

import scala.math._
import scala.swing._
import scala.swing.event._
import rpgboss.lib.Utils._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.uibase._
import rpgboss.editor.misc.GraphicsUtils._
import java.awt.image.BufferedImage
import java.awt.{ Point, Color }
import java.awt.AlphaComposite

class TilesetTileSelector(
  tilesetIndex: Byte,
  tileset: Tileset,
  selectBytesF: Array[Array[Array[Byte]]] => Unit)
  extends BoxPanel(Orientation.Vertical) with TileBytesSelector {

  val imgTileSelector: ImageTileSelector = new ImageTileSelector(tileset.img,
    Tileset.tilesize, Tileset.tilesize, 8, true, true, None) {
    def selectTileF(button: Int, tiles: Array[Array[(Int, Int)]]) = {
      selectBytesF(selectionBytes)
    }
  }

  def selectionBytes = imgTileSelector.selection.map(_.map({
    case (xTile, yTile) =>
      Array(tilesetIndex, xTile.toByte, yTile.toByte)
  }))

  contents += imgTileSelector
}

/**
 * @param   selectTileF         Function called when user selects a new group
 *                              of tiles.
 *
 *                              It's in the same format as how the tiles are
 *                              spatially, i.e.
 *
 *                              [ [(x1, y1), ..., (x2, y1)],
 *                                [          ...          ],
 *                                [(x2, y1), ..., (x2, y2)] ]
 *
 *                              x1 is the smallest tileX index. x2 is largest.
 *                              Same deal with y1 and y2.
 *
 * @param   allowMultiselect    Allow user to select multiple tiles.
 * @param   initialSelection    Defined in tileset space. First pair is the
 *                              x and y of range starts. Second pair is the x
 *                              and y of the range ends.
 */
abstract class ImageTileSelector(
  srcImg: BufferedImage,
  val tilesizeX: Int = 32,
  val tilesizeY: Int = 32,
  val xTilesVisible: Int = 8,
  allowMultiselect: Boolean = true,
  drawSelectionSq: Boolean = true,
  initialSelection: Option[((Int, Int), (Int, Int))] = None)
  extends ScrollPane
  with DisposableComponent {
  horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  verticalScrollBarPolicy = ScrollPane.BarPolicy.Always

  peer.getVerticalScrollBar().setUnitIncrement(16)

  // restrict to 256 by 256 tiles
  val img = srcImg.getSubimage(0, 0,
    min(255 * tilesizeX, srcImg.getWidth),
    min(255 * tilesizeY, srcImg.getHeight))

  val xTilesInTilesetSpace = img.getWidth / tilesizeX

  val imageSlices = ceilIntDiv(img.getWidth / tilesizeX, xTilesVisible)
  val yTilesInSlice = img.getHeight / tilesizeY

  // Set the sizes on the viewport to account for the scrollbars
  val minimumYTilesShown = 2
  val innerSize = new Dimension(
    xTilesVisible * tilesizeX,
    math.min(minimumYTilesShown * tilesizeY, imageSlices * img.getHeight()))
  peer.getViewport().setPreferredSize(innerSize)

  var xRngInSelectorSpace = 0 to 0
  var yRngInSelectorSpace = 0 to 0

  initialSelection map { sel =>
    def toScreenSpace(xTilesetSpace: Int, yTilesetSpace: Int) = {
      val xSS = xTilesetSpace % xTilesVisible
      val ySS = yTilesetSpace + xTilesetSpace / xTilesVisible * yTilesInSlice
      (xSS, ySS)
    }

    val (startPair, endPair) = sel
    val (xSS1, ySS1) = toScreenSpace(startPair._1, startPair._2)
    val (xSS2, ySS2) = toScreenSpace(endPair._1, endPair._2)

    xRngInSelectorSpace = xSS1 to xSS2
    yRngInSelectorSpace = ySS1 to ySS2
  }

  // Defined out here so that subclasses can override it
  def canvasPanelPaintComponent(g: Graphics2D) = {
    g.setComposite(AlphaComposite.SrcOver)

    // Draw checkerboard pattern to indicate transparency
    val checkerL = 8
    val bounds = g.getClipBounds()
    val evenColor = new Color(200, 200, 200)
    val oddColor = new Color(150, 150, 150)
    for (xi <- bounds.x / checkerL to (bounds.x + bounds.width) / checkerL;
         yi <- bounds.y / checkerL to (bounds.y + bounds.height) / checkerL) {
      val color = if ((xi + yi) % 2 == 0) evenColor else oddColor
      g.setColor(color)
      g.fillRect(xi * checkerL, yi * checkerL, checkerL, checkerL)
    }

    for (i <- 0 until imageSlices) {
      val sx1 = i * xTilesVisible * tilesizeX
      val dx2 = math.min(
        xTilesVisible * tilesizeX,
        img.getWidth - sx1)
      val sx2 = math.min(
        (i + 1) * xTilesVisible * tilesizeX,
        img.getWidth)

      g.drawImage(img,
        0, i * img.getHeight,
        dx2, (i + 1) * img.getHeight,
        sx1, 0,
        sx2, img.getHeight,
        null)
    }

    if (drawSelectionSq) {
      TileRect(xRngInSelectorSpace.head, yRngInSelectorSpace.head,
        xRngInSelectorSpace.length, yRngInSelectorSpace.length)
        .optionallyDrawSelRect(g, tilesizeX, tilesizeY)
    }
  }

  val canvasPanel = new Panel() {
    preferredSize = new Dimension(xTilesVisible * tilesizeX,
      imageSlices * img.getHeight)

    background = java.awt.Color.WHITE

    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      canvasPanelPaintComponent(g)
    }
  }

  contents = canvasPanel

  listenTo(canvasPanel.mouse.clicks)
  listenTo(canvasPanel.mouse.moves)

  def toSelTiles(p: Point) =
    (p.getX.toInt / tilesizeX, p.getY.toInt / tilesizeY)

  def toTilesetSpace(selTileX: Int, selTileY: Int) = {
    val tileX = selTileX + selTileY / yTilesInSlice * xTilesVisible
    val tileY = selTileY % yTilesInSlice
    (tileX, tileY)
  }

  /**
   * @param     selectedTiles       This is a 2D array of tile indexes in
   *                                tileset space. The outermost array is the
   *                                rows from top to bottom. The inner arrays
   *                                are from left to right.
   *                                TODO: Simplify this and simply return the
   *                                x and y ranges, since the selections are
   *                                always rectangular.
   */
  def selectTileF(button: Int, selectedTiles: Array[Array[(Int, Int)]])

  def mousePressed(button: Int, xTile: Int, yTile: Int, xInTile: Int,
                   yInTile: Int) = {}

  def selection = yRngInSelectorSpace.map(yTile =>
    xRngInSelectorSpace.map(xTile => toTilesetSpace(xTile, yTile)).toArray)
    .toArray

  // If xTile and yTile are within the bounds of the image in selector space
  def inBounds(xTile: Int, yTile: Int) =
    xTile < xTilesVisible && yTile < imageSlices * yTilesInSlice

  reactions += {
    case e: MousePressed if e.source == canvasPanel => {
      val point = e.point
      val (x1, y1) = toSelTiles(point)
      if (inBounds(x1, y1)) {
        xRngInSelectorSpace = x1 to x1
        yRngInSelectorSpace = y1 to y1
        canvasPanel.repaint()

        val (xTileTS, yTileTS) = toTilesetSpace(x1, y1)
        mousePressed(e.peer.getButton, xTileTS, yTileTS, e.point.x % tilesizeX,
          e.point.y % tilesizeY)

        // If allowed to drag to select multiple ones
        if (allowMultiselect) {
          lazy val temporaryReactions: PartialFunction[Event, Unit] = {
            case MouseDragged(`canvasPanel`, point, _) => {
              val (x2, y2) = toSelTiles(point)
              if (inBounds(x2, y2)) {

                xRngInSelectorSpace = min(x1, x2) to max(x1, x2)
                yRngInSelectorSpace = min(y1, y2) to max(y1, y2)

                canvasPanel.repaint()
              }
            }
            case MouseReleased(`canvasPanel`, point, _, _, _) => {
              selectTileF(e.peer.getButton, selection)
              reactions -= temporaryReactions
            }
          }

          reactions += temporaryReactions
        } else {
          selectTileF(e.peer.getButton, selection)
        }
      }
    }
  }

}
