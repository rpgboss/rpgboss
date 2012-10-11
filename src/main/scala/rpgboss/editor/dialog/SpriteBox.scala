package rpgboss.editor.dialog

import scala.swing._
import scala.swing.event._
import java.awt.Color
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage

class SpriteBox(
    owner: Window, 
    project: Project, 
    initialSpriteSpecOpt: Option[SpriteSpec]) 
  extends Component {
  import Tileset.tilesize
  
  private var spriteSpecOpt = initialSpriteSpecOpt
  private var spriteImg: Option[BufferedImage] = None
  
  /**
   * Updates the cached sprite image used for drawing the component
   */
  def updateImg() = spriteSpecOpt.map { spriteSpec =>
    val spriteset = Spriteset.readFromDisk(project, spriteSpec.spriteset)
    val (xTile, yTile) = spriteset.srcTile(
        spriteSpec.spriteindex,
        Spriteset.DirectionOffsets.SOUTH,
        Spriteset.Steps.STILL)
    spriteset.getTileImage(xTile, yTile)
  }
  updateImg()
  
  val componentW = tilesize*2
  val componentH = tilesize*3
  preferredSize = new Dimension(componentW, componentH)
  maximumSize = new Dimension(componentW*2, componentH*2)
  
  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, peer.getWidth(), peer.getHeight())
    
    // Draw the image centered if it exists
    spriteImg map { img =>
      val dstX = (componentW - img.getWidth())/2
      val dstY = (componentH - img.getHeight())/2
      g.drawImage(img, dstX, dstY, null)
    }
  }
  
  listenTo(this.mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      println("Reaction")
      val diag = new SpriteSelectDialog(
          owner,
          project,
          initialSelection = spriteSpecOpt,
          onSuccess = { selectedSpriteSpecOpt =>
            spriteSpecOpt = selectedSpriteSpecOpt
            updateImg()
          })
      diag.open()
  }
}