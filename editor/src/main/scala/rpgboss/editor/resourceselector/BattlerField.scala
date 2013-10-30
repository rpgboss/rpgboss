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

class BattlerField(
  owner: Window, 
  sm: StateMaster, 
  initial: Option[BattlerSpec], 
  onUpdate: (Option[BattlerSpec]) => Any)
  extends Component {

  private var model: Option[BattlerSpec] = None
  private var battlerImg: Option[BufferedImage] = None
  
  /**
   * Update the model used for drawing the component
   */
  def updateSpriteSpec(valOpt: Option[BattlerSpec]): Unit = {
    model = valOpt
    battlerImg = model.map { battlerSpec =>
      val battler = Battler.readFromDisk(sm.getProj, battlerSpec.name)
      battler.img
    }

    onUpdate(model)

    repaint()
  }

  updateSpriteSpec(initial)

  val componentW = 128
  val componentH = 128
  preferredSize = new Dimension(componentW, componentH)
  maximumSize = new Dimension(componentW * 2, componentH * 2)

  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, peer.getWidth(), peer.getHeight())

    // Draw the image centered if it exists
    battlerImg map { img =>
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