package rpgboss.editor.dialog.db

import com.badlogic.gdx._
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.lib._

object AnimationPlayerGdxPanel {
  def battleback = "defaultrc/battlebg/sys/crownlesswish_rrr.jpg"
  def battlerTarget = "defaultrc/battler/sys/lg/goblinrider.png"

  def width = 640
  def height = 320
}

class AnimationPlayerGdxPanel(project: Project, initial: Encounter)
  extends GdxPanel(AnimationPlayerGdxPanel.width,
                   AnimationPlayerGdxPanel.height) {
  override lazy val gdxListener = new ApplicationAdapter {
    def updateAnimation(animation: Animation) = {

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
}

