package rpgboss.editor.lib

import swing._

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImagePanel(img: BufferedImage) extends Panel {
  val d = new Dimension(img.getWidth(), img.getHeight())

  preferredSize = d

  override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      if (img != null) g.drawImage(img, 0, 0, null)
    }
}
