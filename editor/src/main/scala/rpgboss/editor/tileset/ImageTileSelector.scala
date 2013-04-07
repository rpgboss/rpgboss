package rpgboss.editor.tileset

import scala.math._
import scala.swing._
import scala.swing.event._
import rpgboss.lib.Utils._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._
import java.awt.image.BufferedImage
import java.awt.{Point, Color}
import java.awt.AlphaComposite

class TilesetTileSelector(
    tilesetIndex: Byte,
    tileset: Tileset,
    selectBytesF: Array[Array[Array[Byte]]] => Unit)
    extends BoxPanel(Orientation.Vertical) with TileBytesSelector {
  
  val imgTileSelector = new ImageTileSelector(
      tileset.img,
      _ => selectBytesF(selectionBytes),
      Tileset.tilesize,
      Tileset.tilesize,
      8,
      true,
      true,
      None)
  
  contents += imgTileSelector
  
  def selectionBytes = imgTileSelector.selection.map(_.map({
    case (xTile, yTile) => 
      Array(tilesetIndex, xTile.toByte, yTile.toByte)
  }))
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
 */
class ImageTileSelector(srcImg: BufferedImage,
                        selectTileF: Array[Array[(Int, Int)]] => Unit,
                        val tilesizeX : Int = 32,
                        val tilesizeY : Int = 32,
                        val xTilesVisible : Int = 8,
                        allowMultiselect: Boolean = true,
                        drawSelectionSq: Boolean = true,
                        initialSingleSelTilesetSpace: Option[(Int, Int)] = None)
extends ScrollPane
{ 
  horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  
  peer.getVerticalScrollBar().setUnitIncrement(16)
  
  // restrict to 256 by 256 tiles
  val img = srcImg.getSubimage(0, 0, 
                               min(255*tilesizeX, srcImg.getWidth),
                               min(255*tilesizeY, srcImg.getHeight))
  
  val imageSlices = ceilIntDiv(img.getWidth / tilesizeX, xTilesVisible)
  val yTilesInSlice = img.getHeight / tilesizeY
  
  // Set the sizes on the viewport to account for the scrollbars
  peer.getViewport().setPreferredSize( 
    new Dimension(
        xTilesVisible*tilesizeX, 
        imageSlices*img.getHeight())
  )
  peer.getViewport().setMinimumSize( 
    new Dimension(
        xTilesVisible*tilesizeX, 
        math.min(4*tilesizeY, imageSlices*img.getHeight()))
  )
  
  var xRngInSelectorSpace = 0 to 0
  var yRngInSelectorSpace = 0 to 0
  
  initialSingleSelTilesetSpace map { sel =>
    val (xTS, yTS) = sel
    val xSS = xTS % xTilesVisible
    val ySS = yTS + xTS/xTilesVisible*yTilesInSlice
    xRngInSelectorSpace = xSS to xSS
    yRngInSelectorSpace = ySS to ySS
  }
  
  // Defined out here so that subclasses can override it
  def canvasPanelPaintComponent(g: Graphics2D) = {
    g.setComposite(AlphaComposite.SrcOver)     
    for(i <- 0 until imageSlices) {
      g.drawImage(img, 
                  0, i*img.getHeight,
                  xTilesVisible*tilesizeX, (i+1)*img.getHeight,
                  i*xTilesVisible*tilesizeX, 0,
                  (i+1)*xTilesVisible*tilesizeX, img.getHeight,
                  null)
    }
    
    if(drawSelectionSq) {
      TileRect(xRngInSelectorSpace.head, yRngInSelectorSpace.head,
               xRngInSelectorSpace.length, yRngInSelectorSpace.length)
        .optionallyDrawSelRect(g, tilesizeX, tilesizeY)
    }
  }
  
  val canvasPanel = new Panel() {    
    preferredSize = new Dimension(xTilesVisible*tilesizeX, 
                                  imageSlices*img.getHeight)
    
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      canvasPanelPaintComponent(g)
    }
  }
  
  contents = canvasPanel
  
  listenTo(canvasPanel.mouse.clicks)
  listenTo(canvasPanel.mouse.moves)
  
  def toSelTiles(p: Point) = 
    (p.getX.toInt/tilesizeX, p.getY.toInt/tilesizeY)
  
  def toTilesetSpace(selTileX: Int, selTileY: Int) = {
    val tileX = selTileX + selTileY/yTilesInSlice*xTilesVisible
    val tileY = selTileY % yTilesInSlice
    (tileX, tileY)
  }
  
  def selection = yRngInSelectorSpace.map(yTile => 
      xRngInSelectorSpace.map(xTile => toTilesetSpace(xTile, yTile)).toArray)
      .toArray
  
  def triggerSelectTileF() = selectTileF(selection)
  
  // If xTile and yTile are within the bounds of the image in selector space
  def inBounds(xTile: Int, yTile: Int) =
    xTile < xTilesVisible && yTile < imageSlices*yTilesInSlice
  
  reactions += {
    
    case MousePressed(`canvasPanel`, point, _, _, _) => {
      val (x1, y1) = toSelTiles(point)
      if(inBounds(x1, y1)) {
        xRngInSelectorSpace = x1 to x1
        yRngInSelectorSpace = y1 to y1
        canvasPanel.repaint()
        
        // If allowed to drag to select multiple ones
        if(allowMultiselect) {
          lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
            case MouseDragged(`canvasPanel`, point, _) => {
              val (x2, y2) = toSelTiles(point)
              if(inBounds(x2, y2)) {
                
                xRngInSelectorSpace = min(x1, x2) to max(x1, x2)
                yRngInSelectorSpace = min(y1, y2) to max(y1, y2)
              
                canvasPanel.repaint()
              }
            }
            case MouseReleased(`canvasPanel`, point, _, _, _) => {
              triggerSelectTileF()
              reactions -= temporaryReactions
            }
          }
          
          reactions += temporaryReactions
        } else {
          triggerSelectTileF()
        }
      }
    }
  }
  
}
