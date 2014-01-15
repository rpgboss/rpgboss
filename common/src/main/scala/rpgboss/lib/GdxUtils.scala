package rpgboss.lib

import com.badlogic.gdx.Gdx
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import rpgboss.model.resource.Spriteset
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.Texture.TextureFilter

object GdxUtils {
  /**
   * Run the following on the GUI thread
   */
  def syncRun[T](op: => T): T = {
    val promise = Promise[T]
    val runnable = new Runnable() {
      def run() = {
        promise.success(op)
      }
    }
    Gdx.app.postRunnable(runnable)
    Await.result(promise.future, Duration.Inf)
  }
  
  /**
   * Run the following on the GUI thread
   */
  def asyncRun[T](op: => T): Unit = {
    val runnable = new Runnable() {
      def run() = {
        op
      }
    }
    Gdx.app.postRunnable(runnable)
  }
  
  def generateSpritesTextureAtlas(spritesets: Iterable[Spriteset]) = {
    val packerSprites =
      new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    spritesets.foreach { spriteset =>
      val srcPixmap = new Pixmap(
        Gdx.files.absolute(spriteset.dataFile.getAbsolutePath()))

      val srcFormat = srcPixmap.getFormat()
      if (srcFormat == Pixmap.Format.RGBA8888 ||
        srcFormat == Pixmap.Format.RGBA4444) {

        // Already has transparency. Pack and dispose.
        packerSprites.pack(spriteset.name, srcPixmap)
        srcPixmap.dispose()
      } else if (srcFormat == Pixmap.Format.RGB888) {
        // TODO: Optimize pixel transfer

        // Build transparency from (0, 0) pixel
        val dstPixmap = new Pixmap(
          srcPixmap.getWidth(), srcPixmap.getHeight(), Pixmap.Format.RGBA8888)

        val transparentVal = srcPixmap.getPixel(0, 0)

        for (y <- 0 until srcPixmap.getHeight()) {
          for (x <- 0 until srcPixmap.getWidth()) {
            val curPixel = srcPixmap.getPixel(x, y)

            if (curPixel != transparentVal) {
              dstPixmap.drawPixel(x, y, curPixel)
            }
          }
        }

        packerSprites.pack(spriteset.name, dstPixmap)
        srcPixmap.dispose()
        dstPixmap.dispose()
      }
    }

    // TODO: Re-enable logging in a consistent way
//    logger.info("Packed sprites into %d pages".format(
//      packerSprites.getPages().size))
      
    packerSprites.generateTextureAtlas(
      TextureFilter.Nearest, TextureFilter.Nearest, false)
  }
}