package rpgboss.editor.resourceselector

import scala.swing._
import scala.swing.event._
import java.awt.Color
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster
import rpgboss.editor.resourceselector._
import java.awt.Dimension
import java.awt.Graphics2D
import rpgboss.model.resource.Tileset.tilesize
import rpgboss.editor.imageset.selector.SpriteSelector

class SpriteField(
  owner: Window, 
  sm: StateMaster, 
  initialSpriteSpecOpt: Option[SpriteSpec], 
  onUpdate: (Option[SpriteSpec]) => Any)
  extends Component {
  import Tileset.tilesize

  private var spriteSpecOpt: Option[SpriteSpec] = None
  private var spriteImg: Option[BufferedImage] = None

  def getSpriteSpec = spriteSpecOpt
  
  /**
   * Updates the cached sprite image used for drawing the component
   */
  def updateSpriteSpec(s: Option[SpriteSpec]): Unit = {
    spriteSpecOpt = s
    spriteImg = spriteSpecOpt.map { spriteSpec =>
      val spriteset = Spriteset.readFromDisk(sm.getProj, spriteSpec.spriteset)
      spriteset.srcTileImg(spriteSpec)
    }

    onUpdate(spriteSpecOpt)

    SpriteField.this.repaint()
  }

  updateSpriteSpec(initialSpriteSpecOpt)

  val componentW = tilesize * 2
  val componentH = tilesize * 3
  preferredSize = new Dimension(componentW, componentH)
  maximumSize = new Dimension(componentW * 2, componentH * 2)

  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, peer.getWidth(), peer.getHeight())

    // Draw the image centered if it exists
    spriteImg map { img =>
      val dstX = (componentW - img.getWidth()) / 2
      val dstY = (componentH - img.getHeight()) / 2
      g.drawImage(img, dstX, dstY, null)
    }
  }

  listenTo(SpriteField.this.mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      val diag = new SpriteSelectDialog(owner, sm, spriteSpecOpt) {
        def onSuccess(result: Option[SpriteSpec]) =
          updateSpriteSpec(result)
      }
      diag.open()
  }
}

abstract class SpriteSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SpriteSpec])
extends ResourceSelectDialog(owner, sm, initial, true, Spriteset) {
  import Spriteset._

  def specToResourceName(spec: SpriteSpec): String = spec.spriteset
  def newRcNameToSpec(name: String, prevSpec: Option[SpriteSpec]) = {
    val (dir, step) = prevSpec.map { spec =>
      (spec.dir, spec.step)
    } getOrElse ((SpriteSpec.Directions.SOUTH, SpriteSpec.Steps.STILL))

    val idx = prevSpec.map { spec =>
      val newSpriteset = sm.assetCache.getSpriteset(name)
      val prevIdx = spec.spriteIndex

      // ensure that the prevIdx is applicable to the new spriteset
      if (prevIdx < newSpriteset.nSprites) prevIdx else 0
    } getOrElse (0)

    SpriteSpec(name, idx, dir, step)
  }

  def rightPaneDim = new Dimension(384, 384)

  override def rightPaneFor(
    selection: SpriteSpec,
    updateSelectionF: SpriteSpec => Unit) = {
    val spriteset = sm.assetCache.getSpriteset(selection.spriteset)

    new SpriteSelector(spriteset, selection, updateSelectionF)
  }
}