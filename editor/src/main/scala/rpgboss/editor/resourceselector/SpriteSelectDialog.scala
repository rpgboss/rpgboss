package rpgboss.editor.resourceselector
import scala.swing._
import rpgboss.editor.misc.SwingUtils._
import rpgboss.editor.uibase._
import rpgboss.editor.resourceselector._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.imageset.selector.SpriteSelector
import java.awt.Dimension
import rpgboss.editor.StateMaster

class SpriteSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SpriteSpec])
extends ResourceSelectDialogBase(owner, Spriteset) {
  override val resourceSelector =
    new SpriteSelectPanel(sm, initial)
}

class SpriteSelectPanel(
  sm: StateMaster,
  initialSelectionOpt: Option[SpriteSpec])
  extends ResourceSelectPanel[SpriteSpec, Spriteset, SpritesetMetadata](
    sm,
    initialSelectionOpt,
    true,
    Spriteset) {
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

  override def rightPaneDim = new Dimension(
    384, 384)

  override def rightPaneFor(
    selection: SpriteSpec,
    updateSelectionF: SpriteSpec => Unit) = {
    val spriteset = sm.assetCache.getSpriteset(selection.spriteset)

    new SpriteSelector(spriteset, selection, updateSelectionF)
  }
}