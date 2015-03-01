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
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp

abstract class ImageResourceField[SpecT](
  owner: Window,
  sm: StateMaster,
  initial: Option[SpecT],
  onUpdate: (Option[SpecT]) => Any)
  extends Component {

  private var model: Option[SpecT] = None
  private var image: Option[BufferedImage] = None

  def getValue = model

  // Implementing classes provide an image representation for the spec
  def getImage(s: SpecT): BufferedImage
  def componentW: Int
  def componentH: Int
  def getSelectDialog(): Dialog

  /**
   * Updates the model and the representative image used to draw the component
   */
  def updateModel(s: Option[SpecT]): Unit = {
    model = s
    image = s.map(getImage _)
    onUpdate(s)
    repaint()
  }

  updateModel(initial)

  preferredSize = new Dimension(componentW, componentH)
  maximumSize = new Dimension(componentW * 2, componentH * 2)

  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    if (enabled)
      g.setColor(Color.WHITE)
    else
      g.setColor(Color.GRAY)
    g.fillRect(0, 0, peer.getWidth(), peer.getHeight())

    // Draw the image centered if it exists
    image map { img =>
      val dstX = (componentW - img.getWidth()) / 2
      val dstY = (componentH - img.getHeight()) / 2
      g.drawImage(img, dstX, dstY, null)
    }
  }

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked => {
      if (enabled)
        getSelectDialog().open()
    }
  }
}

class AnimationImageField(
  owner: Window,
  sm: StateMaster,
  initial: Option[AnimationVisual],
  onUpdate: Option[AnimationVisual] => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {
  def getImage(s: AnimationVisual) = {
    val animationImage =
      AnimationImage.readFromDisk(sm.getProj, s.animationImage)
    animationImage.getImageForFrame(s.start.frameIndex)
  }

  def componentW = AnimationImage.tilesize
  def componentH = AnimationImage.tilesize

  def getSelectDialog() = {
    new AnimationImageSelectDialog(owner, sm, getValue, updateModel _)
  }
}

class FaceField(
  owner: Window,
  sm: StateMaster,
  initial: Option[FaceSpec],
  onUpdate: (Option[FaceSpec]) => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {

  def getImage(f: FaceSpec): BufferedImage = {
    val faceset = Faceset.readFromDisk(sm.getProj, f.faceset)
    faceset.getTileImage(f.faceX, f.faceY)
  }

  def componentW = Faceset.canonicalTileSize
  def componentH = Faceset.canonicalTileSize

  def getSelectDialog() =
    new FaceSelectDialog(owner, sm, getValue) {
      def onSuccess(result: Option[FaceSpec]) =
        updateModel(result)
    }
}

class SpriteField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SpriteSpec],
  onUpdate: (Option[SpriteSpec]) => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {

  def getImage(s: SpriteSpec): BufferedImage = {
    val spriteset = Spriteset.readFromDisk(sm.getProj, s.name)
    spriteset.srcTileImg(s)
  }

  def componentW = tilesize * 1
  def componentH = tilesize * 2

  def getSelectDialog() =
    new SpriteSelectDialog(owner, sm, getValue) {
      def onSuccess(result: Option[SpriteSpec]) =
        updateModel(result)
    }
}

object BattlerField {
  def getImage(m: BattlerSpec, proj: Project): BufferedImage = {
    val orig = Battler.readFromDisk(proj, m.name).img
    val tx = new AffineTransform()
    tx.scale(m.scale, m.scale);

    val op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
    op.filter(orig, null);
  }
}

class BattlerField(
  owner: Window,
  sm: StateMaster,
  initial: Option[BattlerSpec],
  onUpdate: (Option[BattlerSpec]) => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {

  def componentW = 128
  def componentH = 128

  def getImage(m: BattlerSpec): BufferedImage =
    BattlerField.getImage(m, sm.getProj)

  def getSelectDialog() =
    new BattlerSelectDialog(owner, sm, getValue) {
      def onSuccess(result: Option[BattlerSpec]) =
        updateModel(result)
    }
}