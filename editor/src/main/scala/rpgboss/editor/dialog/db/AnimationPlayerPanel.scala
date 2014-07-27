package rpgboss.editor.dialog.db

import com.badlogic.gdx._
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.lib._
import scala.swing._

object AnimationPlayerGdxPanel {
  def battleback = "defaultrc/battlebg/sys/crownlesswish_rrr.jpg"
  def battlerTarget = "defaultrc/battler/sys/lg/goblinrider.png"

  def width = 640
  def height = 320
}

case class AnimationPlayerStatus(
  var playing: Boolean, var currentTime: Float, var totalTime: Float)

class AnimationPlayerGdxPanel(
  project: Project,
  onStatusUpdate: AnimationPlayerStatus => Unit)
  extends GdxPanel(project, AnimationPlayerGdxPanel.width,
    AnimationPlayerGdxPanel.height) {
  override lazy val gdxListener = new ApplicationAdapter {
    var status = AnimationPlayerStatus(false, 0, 0)

    def updateAnimation(animation: Animation) = {

    }

    def play() = {

    }

    private def sendStatusUpdate() = {
      Swing.onEDT(status.copy())
    }

    override def create() = {

    }

    override def render() = {

    }
  }

  def updateAnimation(animation: Animation) = GdxUtils.asyncRun {
    // Make a defensive copy, as we are sending it to a different thread
    gdxListener.updateAnimation(Utils.deepCopy(animation))
  }

  def play() = GdxUtils.asyncRun { gdxListener.play() }
}

class AnimationPlayerPanel(project: Project, animation: Animation)
  extends BoxPanel(Orientation.Vertical){

  val gdxPanel = new AnimationPlayerGdxPanel(project, onStatusUpdate)
  val btnPlay = new Button(Action("Play") { gdxPanel.play() })

  val lblStatus = new Label

  def onStatusUpdate(status: AnimationPlayerStatus): Unit = {
    btnPlay.enabled = !status.playing
    btnPlay.text = if (status.playing) "Playing" else "Play"

    lblStatus.text = "%f / %f s".format(status.currentTime, status.totalTime)
  }

  contents += gdxPanel
  contents += new BoxPanel(Orientation.Horizontal) {
    contents += btnPlay
    contents += lblStatus
  }
}