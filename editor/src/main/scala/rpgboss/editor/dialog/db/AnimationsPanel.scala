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
    val fVisuals = new TableEditor[AnimationVisual]() {
      val buffer = ArrayBuffer(model.visuals: _*)
      def title = "Visuals"
      def modelArray = buffer
      def newInstance() = AnimationVisual()
      def colHeaders = Array("Start", "End", "Animation File")
      def getRowStrings(visual: AnimationVisual) = {
        Array(visual.startTime.toString, visual.endTime.toString,
          visual.animationName)
      }
      def onUpdate() =
        model.visuals = modelArray.toArray
      def showEditDialog(initial: AnimationVisual,
        okCallback: AnimationVisual => Unit) = {

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

      }
    }

    new BoxPanel(Orientation.Horizontal) {
      contents += fVisuals
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

  val fStartTime =
    new FloatSpinner(model.startTime, 0f, 30f, model.startTime = _, 0.1f)
  val fEndTime =
    new FloatSpinner(model.endTime, 0f, 30f, model.endTime = _, 0.1f)


  contents = new DesignGridPanel {

    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    val start = model.startTime
    val end = model.endTime
    model.startTime = math.min(start, end)
    model.endTime = math.max(start, end)

    onOk(model)
    close()
  }
}