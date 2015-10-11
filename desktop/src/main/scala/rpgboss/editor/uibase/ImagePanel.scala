package rpgboss.editor.uibase

import swing._
import java.awt.image.BufferedImage

class ImagePanel(img: BufferedImage) extends Panel {
  preferredSize =
    new Dimension(math.min(400, img.getWidth()), math.min(400, img.getHeight()))

  var tintColor = new Color(0, 0, 0, 0)
  override def paintComponent(g: Graphics2D) =
  {
    super.paintComponent(g)
    if (img != null) g.drawImage(img, 0, 0, null)
    g.setColor(tintColor)
    g.fill(g.getClip())
  }
}