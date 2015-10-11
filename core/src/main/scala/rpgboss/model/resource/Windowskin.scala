package rpgboss.model.resource

import rpgboss.lib._
import rpgboss.model._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._
import java.awt.image.BufferedImage
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.Texture

case class WindowskinMetadata()

case class Windowskin(proj: Project, name: String,
                      metadata: WindowskinMetadata)
  extends TiledImageResource[Windowskin, WindowskinMetadata] {
  def meta = Windowskin
  def tileH = 16
  def tileW = 16

  /**
   * Draw the window using libgdx commands
   * @param batch     The SpriteBatch instance used to draw
   * @param region    The TextureRegion with the window skin
   * @param x         Destination x
   * @param y         Destination y
   * @param w         Destination width
   * @param h         Destination height
   */
  def draw(
    batch: SpriteBatch,
    texture: Texture,
    x: Float, y: Float,
    w: Float, h: Float,
    bordersOnly: Boolean = false) = {

    import math._

    /**
     * Draws the subimage specified at srcX, srcY, srcW, and srcH
     * at dstX, dstY, dstW, and dstH, in units of 1 pixels.
     *
     * dstX and dstY are with respect to the origin at (x, y)
     */
    def drawSubimage(
      srcX: Int, srcY: Int, srcW: Int, srcH: Int,
      dstX: Float, dstY: Float, dstW: Float, dstH: Float) = {
      batch.draw(
        texture,
        x + dstX, y + dstY, dstW, dstH,
        srcX, srcY, srcW, srcH,
        false, true)
    }

    /**
     * Variant of drawSubimage. Source coordinates are in units of 16 pixels.
     */
    def drawSubimage16(
      srcXp: Int, srcYp: Int, srcWp: Int, srcHp: Int,
      dstX: Float, dstY: Float, dstW: Float = 16, dstH: Float = 16) =
      drawSubimage(srcXp * 16, srcYp * 16, srcWp * 16, srcHp * 16,
        dstX, dstY, dstW, dstH)

    // Don't draw the background if it's an uber-small box.
    if (!bordersOnly && w >= 32 && h >= 32) {
      // Draw the stretched background
      drawSubimage(0, 0, 64, 64, 0, 0, w, h)

      // Draw the tiled background
      for (i <- 0 until ceilFloatDiv(w.toInt, 64);
           j <- 0 until ceilFloatDiv(h, 64)) {
        val wToDraw = min(64, w - i * 64).round
        val hToDraw = min(64, h - j * 64).round
        // Tiled background origin at (0, 64)
        drawSubimage(0, 64, wToDraw, hToDraw, i * 64, j * 64, wToDraw, hToDraw)
      }
    }

    // paint borders
    if (w >= 16 && h >= 16) {
      drawSubimage16(4, 0, 1, 1, 0, 0) // NW
      drawSubimage16(7, 0, 1, 1, w - 16, 0) // NE
      drawSubimage16(4, 3, 1, 1, 0, h - 16) // SW
      drawSubimage16(7, 3, 1, 1, w - 16, h - 16) // SE
    }

    if (w >= 16) {
      drawSubimage16(5, 0, 2, 1, 16, 0, w - 32, 16) // N

      if (h >= 16) {
        drawSubimage16(5, 3, 2, 1, 16, h - 16, w - 32, 16) // S
      }
    }

    if (h >= 16) {
      drawSubimage16(4, 1, 1, 2, 0, 16, 16, h - 32) // E

      if (w >= 16) {
        drawSubimage16(7, 1, 1, 2, w - 16, 16, 16, h - 32) // W
      }
    }
  }

  /**
   * Draw a square block at srcXBlock and srcYBlock of size blockSize
   */
  private def drawBlock(
    batch: SpriteBatch,
    texture: Texture,
    dstX: Float, dstY: Float,
    dstW: Float, dstH: Float,
    blockSize: Int,
    srcXBlock: Int, srcYBlock: Int) = {
    batch.draw(
      texture,
      dstX, dstY,
      dstW, dstH,
      srcXBlock * blockSize,
      srcYBlock * blockSize,
      blockSize, blockSize,
      false, true)
  }

  def drawCursor(
    batch: SpriteBatch,
    texture: Texture,
    dstX: Float, dstY: Float) =
    drawBlock(batch, texture, dstX, dstY, 32f, 32f, 32, 2, 3)

  def drawArrow(
    batch: SpriteBatch,
    texture: Texture,
    dstX: Float, dstY: Float) =
    drawBlock(batch, texture, dstX, dstY, 16f, 16f, 16, 6, 4)
}

object Windowskin extends MetaResource[Windowskin, WindowskinMetadata] {
  def rcType = "windowskin"
  def keyExts = Array("png")

  def defaultInstance(proj: Project, name: String) =
    Windowskin(proj, name, WindowskinMetadata())
}
