package rpgboss.player.entity

import scala.concurrent.Channel

import com.badlogic.gdx.graphics.g2d.SpriteBatch

import rpgboss.lib.GdxUtils.syncRun
import rpgboss.lib.Rect
import rpgboss.lib.Utils
import rpgboss.model.SoundSpec
import rpgboss.model.resource.Sound
import rpgboss.player.ChoiceInputHandler
import rpgboss.player.InputMultiplexer
import rpgboss.player.MyKeys
import rpgboss.player.MyKeys.Cancel
import rpgboss.player.MyKeys.Down
import rpgboss.player.MyKeys.Left
import rpgboss.player.MyKeys.OK
import rpgboss.player.MyKeys.Right
import rpgboss.player.MyKeys.Up
import rpgboss.player.PersistentState
import rpgboss.player.WindowManager

abstract class ChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  rect: Rect,
  invisible: Boolean = false,
  defaultChoice: Int = 0,
  allowCancel: Boolean = true)
  extends Window(manager, inputs, rect, invisible)
  with ChoiceInputHandler {

  protected var curChoice = defaultChoice

  override def capturedKeys =
    Set(MyKeys.Left, MyKeys.Right, MyKeys.Up, MyKeys.Down,
        MyKeys.OK, MyKeys.Cancel)

  val choiceChannel = new Channel[Int]()

  def project = manager.project
  def assets = manager.assets

  override def startClosing() = {
    super.startClosing()
    choiceChannel.write(-1)
  }

  def optionallyReadAndLoad(spec: Option[SoundSpec]) = {
    val snd = spec.map(s => Sound.readFromDisk(project, s.sound))
    snd.map(_.loadAsset(assets))
    snd
  }

  val soundSelect = optionallyReadAndLoad(project.data.startup.soundSelect)
  val soundCursor = optionallyReadAndLoad(project.data.startup.soundCursor)
  val soundCancel = optionallyReadAndLoad(project.data.startup.soundCancel)
  val soundCannot = optionallyReadAndLoad(project.data.startup.soundCannot)

  class ChoiceWindowScriptInterface extends WindowScriptInterface {
    def getChoice() = choiceChannel.read

    def takeFocus(): Unit = syncRun {
      inputs.remove(ChoiceWindow.this)
      inputs.prepend(ChoiceWindow.this)
      manager.focusWindow(ChoiceWindow.this)
    }
  }

  override lazy val scriptInterface = new ChoiceWindowScriptInterface
}

/**
 * @param   choices     Is an Array[Set[Rect]] to support some choices being
 *                      defined by multiple rectangles on screen. For instance,
 *                      selecting all the members of your party during a battle.
 */
class SpatialChoiceWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  choices: Array[Set[Rect]] = Array(),
  defaultChoice: Int = 0)
  extends ChoiceWindow(persistent, manager, inputs, Rect(0, 0, 0, 0),
                       invisible = true, defaultChoice, allowCancel = true) {
  def keyActivate(key: Int): Unit = {
    import MyKeys._

    if (state != Window.Open)
      return

    // TODO: Remove hack
    // Need to finish loading all assets before accepting key input
    assets.finishLoading()

    import MyKeys._
    if (key == Up || key == Left) {
      curChoice = Utils.pmod(curChoice - 1, choices.length)
      soundCursor.map(_.getAsset(assets).play())
    } else if (key == Down || key == Right) {
      curChoice = Utils.pmod(curChoice + 1, choices.length)
      soundCursor.map(_.getAsset(assets).play())
    }

    if (key == OK) {
      soundSelect.map(_.getAsset(assets).play())
      choiceChannel.write(curChoice)
    }

    if (key == Cancel) {
      soundCancel.map(_.getAsset(assets).play())
      choiceChannel.write(-1)
    }
  }

  override def render(b: SpriteBatch): Unit = {
    // Draw the window and text
    super.render(b)

    if (curChoice >= choices.length || curChoice < 0)
      return

    for (choiceRect <- choices(curChoice)) {
      skin.draw(b, skinRegion,
                choiceRect.left, choiceRect.top, choiceRect.w, choiceRect.h,
                bordersOnly = true)
    }
  }
}