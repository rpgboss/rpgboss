package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.model._
import rpgboss.model.Constants._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._
import scala.collection.mutable.ArrayBuffer
import rpgboss.lib.Utils

class AnimationsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Animations",
    dbDiag.model.enums.animations) {
  def panelName = "Animations"
  def newDefaultInstance() = new Animation()

  def editPaneForItem(idx: Int, model: Animation) = {
    logger.debug("New edit pane for animationId=%d".format(idx))
    val fName = textField(model.name, v => {
      model.name = v
      refreshModel()
    })

    val fVisuals = new TableEditor[AnimationVisual]() {
      val buffer = ArrayBuffer(model.visuals: _*)
      def title = "Visuals"
      def modelArray = buffer
      def newInstance() = AnimationVisual()
      def colHeaders = Array("Start", "End", "Animation File")
      def getRowStrings(visual: AnimationVisual) = {
        Array(visual.start.time.toString, visual.end.time.toString,
          visual.animationImage)
      }
      def onUpdate() =
        model.visuals = modelArray.toArray
      def showEditDialog(initial: AnimationVisual,
        okCallback: AnimationVisual => Unit) = {
        val d = new AnimationVisualDialog(owner, sm, initial, okCallback)
        d.open()
      }
    }

    val fSounds = new TableEditor[AnimationSound]() {
      val buffer = ArrayBuffer(model.sounds: _*)
      def title = "Sounds"
      def modelArray = buffer
      def newInstance() = AnimationSound()
      def colHeaders = Array("Start", "Sound")
      def getRowStrings(item: AnimationSound) = {
        Array(item.time.toString, item.sound.sound)
      }
      def onUpdate() =
        model.sounds = modelArray.toArray
      def showEditDialog(initial: AnimationSound,
        okCallback: AnimationSound => Unit) = {
        val d = new AnimationSoundDialog(owner, sm, initial, okCallback)
        d.open()
      }
    }

    val animationPlayerPanel = new AnimationPlayerPanel(sm.getProj, model)

    new BoxPanel(Orientation.Vertical) with DisposableComponent {
      contents += new DesignGridPanel {
        row().grid(lbl("Name:")).add(fName)
      }
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += animationPlayerPanel
        contents += new BoxPanel(Orientation.Vertical) {
          contents += fVisuals
          contents += fSounds
        }
      }

      override def dispose() = {
        animationPlayerPanel.dispose()
        super.dispose()
      }
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.animations = dataAsArray
  }
}

class AnimationVisualDialog(
  owner: Window,
  sm: StateMaster,
  initial: AnimationVisual,
  onOk: (AnimationVisual) => Unit)
  extends StdDialog(owner, "Animation Visual") {
  import SwingUtils._

  val model = Utils.deepCopy(initial)

  // These are declared first because fAnimationImage refers to these
  val fStartFrame = new AnimationKeyframePanel(model.start)
  val fEndFrame = new AnimationKeyframePanel(model.end)

  val fAnimationImage = new AnimationImageField(
    owner,
    sm,
    if (initial.animationImage.isEmpty) None else Some(initial),
    newSelectionOpt => {
      newSelectionOpt.map { newSelection =>
        fStartFrame.fFrameIndex.setValue(newSelection.start.frameIndex)
        fEndFrame.fFrameIndex.setValue(newSelection.end.frameIndex)

        model.animationImage = newSelection.animationImage
      }
    })

  contents = new DesignGridPanel {
    row().grid(leftLabel("Image:")).add(fAnimationImage)

    row().grid(leftLabel("Start:")).add(fStartFrame)
    row().grid(leftLabel("End:")).add(fEndFrame)

    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    val start = model.start.time
    val end = model.end.time
    model.start.time = math.min(start, end)
    model.end.time = math.max(start, end)

    onOk(model)
    close()
  }
}

/**
 * Modifies |model| in-place.
 */
class AnimationKeyframePanel(model: AnimationKeyframe) extends DesignGridPanel {
  val fStartTime =
    new FloatSpinner(model.time, 0f, 30f, model.time = _, 0.1f)
  val fFrameIndex =
    new NumberSpinner(model.frameIndex, 0, 100, model.frameIndex = _)
  val fX = new NumberSpinner(model.x, -999, 999, model.x = _)
  val fY = new NumberSpinner(model.y, -999, 999, model.y = _)

  row().grid(leftLabel("Time:")).add(fStartTime)
  row().grid(leftLabel("Frame index:")).add(fFrameIndex, 2)
  row().grid(leftLabel("x:")).add(fX).grid(leftLabel("y:")).add(fY)
}

class AnimationSoundDialog(
  owner: Window,
  sm: StateMaster,
  initial: AnimationSound,
  onOk: (AnimationSound) => Unit)
  extends StdDialog(owner, "Animation Sound") {
  import SwingUtils._

  val model = Utils.deepCopy(initial)
  val fTime = new FloatSpinner(model.time, 0f, 30f, model.time = _, 0.1f)
  val fSound = {
    val initialModel =
      if (model.sound.sound.isEmpty()) None else Some(model.sound)
    new SoundField(
      owner, sm, initialModel, _.map(model.sound = _), allowNone = false)
  }

  contents = new DesignGridPanel {
    row().grid(leftLabel("Start time:")).add(fTime)
    row().grid(leftLabel("Sound:")).add(fSound)

    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    onOk(model)
    close()
  }
}