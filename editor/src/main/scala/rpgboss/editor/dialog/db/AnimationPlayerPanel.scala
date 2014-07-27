package rpgboss.editor.dialog.db

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.player.entity.AnimationPlayer
import rpgboss.lib._
import scala.swing._
import rpgboss.player.GdxGraphicsUtils
import com.badlogic.gdx.math.Matrix4

object AnimationPlayerGdxPanel {
  def battleback = "sys/crownlesswish_rrr.jpg"
  def battlerTarget = "sys/lg/goblinrider.png"

  def width = 640
  def height = 320
}

case class AnimationPlayerStatus(
  var playing: Boolean, var currentTime: Float, var totalTime: Float)

class AnimationPlayerGdxPanel(
  project: Project,
  initialAnimation: Animation,
  onStatusUpdate: AnimationPlayerStatus => Unit)
  extends GdxPanel(project, AnimationPlayerGdxPanel.width,
    AnimationPlayerGdxPanel.height) {

  /**
   * Constructor runs on the Swing main thread. Methods run on the Gdx thread.
   */
  override lazy val gdxListener = new ApplicationAdapter {
    var status = AnimationPlayerStatus(false, 0, 0)
    var animationPlayer: AnimationPlayer = null

    val background =
      BattleBackground.readFromDisk(project, AnimationPlayerGdxPanel.battleback)
    val battler =
      Battler.readFromDisk(project, AnimationPlayerGdxPanel.battlerTarget)
    var batch: SpriteBatch = null

    def updateAnimation(animation: Animation) = {
      animationPlayer = new AnimationPlayer(project, animation, assets)
      status.totalTime = animation.totalTime
    }

    def play() = {
      assert(animationPlayer != null)
      animationPlayer.play()
    }

    override def create() = {
      background.loadAsset(assets)
      battler.loadAsset(assets)

      batch = new SpriteBatch()
      batch.enableBlending()

      // Again, setting the projection matrix because it seems to work...
      val matrix = batch.getTransformMatrix()
      matrix.trn(AnimationPlayerGdxPanel.width / 2,
          AnimationPlayerGdxPanel.height / 2, 0)
      batch.setTransformMatrix(matrix)
    }

    override def dispose() = {
      background.unloadAsset(assets)
      battler.unloadAsset(assets)
      super.dispose()
    }

    override def render() = {
      // Only do anything if the assets have been loaded.
      if (assets.update()) {
        animationPlayer.update(Gdx.graphics.getDeltaTime())

        // Send status update to the Swing thread
        status.playing = animationPlayer.playing
        status.currentTime = animationPlayer.time

        val statusCopy = status.copy()
        Swing.onEDT({ onStatusUpdate(statusCopy) })

        // Render the graphics.
        batch.begin()

        GdxGraphicsUtils.drawCentered(batch, background.getAsset(assets), 0, 0)
        GdxGraphicsUtils.drawCentered(batch, battler.getAsset(assets), 0, 0)

        animationPlayer.render(batch)

        batch.end()
      }
    }
  }

  def updateAnimation(animation: Animation) = GdxUtils.asyncRun {
    // Make a defensive copy, as we are sending it to the Gdx thread,
    // while the user could continue to make modifications on the Swing thread.
    gdxListener.updateAnimation(Utils.deepCopy(animation))
  }

  def play(animation: Animation) = {
    updateAnimation(animation)
    GdxUtils.asyncRun { gdxListener.play() }
  }

  updateAnimation(initialAnimation)
}

class AnimationPlayerPanel(project: Project, animation: Animation)
  extends BoxPanel(Orientation.Vertical){

  val gdxPanel = new AnimationPlayerGdxPanel(project, animation, onStatusUpdate)
  val btnPlay = new Button(Action("Play") { gdxPanel.play(animation) })

  val lblStatus = new Label

  def onStatusUpdate(status: AnimationPlayerStatus): Unit = {
    btnPlay.enabled = !status.playing
    btnPlay.text = if (status.playing) "Playing" else "Play"

    lblStatus.text = "%f / %f s".format(status.currentTime, status.totalTime)
  }

  contents += new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += gdxPanel
    contents += Swing.HGlue
  }
  contents += new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += btnPlay
    contents += lblStatus
    contents += Swing.HGlue
  }
}