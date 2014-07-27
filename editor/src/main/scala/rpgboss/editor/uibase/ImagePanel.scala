package rpgboss.editor.uibase

import swing._
import java.awt.image.BufferedImage

class ImagePanel(img: BufferedImage) extends ScrollPane {
  preferredSize =
    new Dimension(math.min(400, img.getWidth()), math.min(400, img.getHeight()))

  contents = new Panel {
    preferredSize = new Dimension(img.getWidth(), img.getHeight())
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      if (img != null) g.drawImage(img, 0, 0, null)
    }
  }
}
